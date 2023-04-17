/**
 *  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 *  with the License. A copy of the License is located at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  or in the 'license' file accompanying this file. This file is distributed on an 'AS IS' BASIS, WITHOUT WARRANTIES
 *  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific language governing permissions
 *  and limitations under the License.
 */

package sofeware.aws.solution.clickstream;

import com.maxmind.db.MaxMindDbConstructor;
import com.maxmind.db.MaxMindDbParameter;
import com.maxmind.db.Reader;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.SparkFiles;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.api.java.UDF2;
import org.apache.spark.sql.catalyst.expressions.GenericRow;
import org.apache.spark.sql.expressions.UserDefinedFunction;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.udf;
import java.io.File;
import java.net.InetAddress;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class IPEnrichment {
    public Dataset<Row> transform(final Dataset<Row> dataset) {
        UserDefinedFunction udfEnrichIP = udf(enrich(), DataTypes.createStructType(
                new StructField[]{
                        DataTypes.createStructField("city", DataTypes.StringType, true),
                        DataTypes.createStructField("continent", DataTypes.StringType, true),
                        DataTypes.createStructField("country", DataTypes.StringType, true),

                        DataTypes.createStructField("metro", DataTypes.StringType, true),
                        DataTypes.createStructField("region", DataTypes.StringType, true),
                        DataTypes.createStructField("sub_continent", DataTypes.StringType, true),
                }
        ));
        return dataset.withColumn("geo",
                udfEnrichIP.apply(
                        col("geo_for_enrich").getItem("ip"),
                        col("geo_for_enrich").getItem("locale")
                )
        );
    }

    private UDF2<String, String, Row> enrich() {
        return (ipValue, localeValue) -> {
            GenericRow defaultRow = new GenericRow(
                    new Object[]{"", "", "", "", "", ""}
            );
            try (Reader reader = new Reader(new File(SparkFiles.get("GeoLite2-City.mmdb")))) {
                InetAddress address = InetAddress.getByName(ipValue);
                LookupResult result = reader.get(address, LookupResult.class);
                return Optional.ofNullable(result)
                        .map(geo -> new GenericRow(new Object[]{
                                Optional.ofNullable(geo.getCity()).map(LookupResult.City::getName).orElse(""),
                                Optional.ofNullable(geo.getContinent()).map(LookupResult.Continent::getName).orElse(""),
                                Optional.ofNullable(geo.getCountry()).map(LookupResult.Country::getName).orElse(""),
                                "",
                                "",
                                ""
                        }))
                        .orElse(defaultRow);
            } catch (Exception e) {
                log.warn(e.getMessage());
                return defaultRow;
            }
        };
    }

    public static class LookupResult {

        public static class Country {
            private final String name;
            private final String isoCode;

            @MaxMindDbConstructor
            public Country(final @MaxMindDbParameter(name = "names") Map<String, String> names,
                           final @MaxMindDbParameter(name = "iso_code") String isoCode) {
                this.name = names.getOrDefault("en", "");
                this.isoCode = isoCode;
            }

            public String getName() {
                return name;
            }

            public String getIsoCode() {
                return this.isoCode;
            }
        }

        public static class Continent {
            @Getter
            private final String name;

            @MaxMindDbConstructor
            public Continent(final @MaxMindDbParameter(name = "names") Map<String, String> names) {
                this.name = names.getOrDefault("en", "");
            }
        }

        public static class City {
            @Getter
            private final String name;

            @MaxMindDbConstructor
            public City(final @MaxMindDbParameter(name = "names") Map<String, String> names) {
                this.name = names.getOrDefault("en", "");
            }
        }

        public static class Location {
            @Getter
            private final double latitude;
            @Getter
            private final double longitude;

            @MaxMindDbConstructor
            public Location(final @MaxMindDbParameter(name = "latitude") double latitude,
                            final @MaxMindDbParameter(name = "longitude") double longitude) {
                this.latitude = latitude;
                this.longitude = longitude;
            }
        }

        @Getter
        private final Country country;
        @Getter
        private final Continent continent;
        @Getter
        private final City city;
        @Getter
        private final Location location;

        @MaxMindDbConstructor
        public LookupResult(final @MaxMindDbParameter(name = "country") Country country,
                            final @MaxMindDbParameter(name = "continent") Continent continent,
                            final @MaxMindDbParameter(name = "city") City city,
                            final @MaxMindDbParameter(name = "location") Location location) {
            this.country = country;
            this.continent = continent;
            this.city = city;
            this.location = location;
        }
    }
}
