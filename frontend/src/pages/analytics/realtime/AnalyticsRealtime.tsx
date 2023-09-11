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

import { AppLayout } from '@cloudscape-design/components';
import Loading from 'components/common/Loading';
import Navigation from 'components/layouts/Navigation';
import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';

const AnalyticsRealtime: React.FC = () => {
  const { projectId, appId } = useParams();
  const [loadingData, setLoadingData] = useState(false);

  useEffect(() => {
    setLoadingData(true);
    setLoadingData(false);
  }, []);

  return (
    <AppLayout
      toolsHide
      content={
        loadingData ? (
          <Loading />
        ) : (
          <div id={'qs-container'} className="iframe-dashboard"></div>
        )
      }
      headerSelector="#header"
      navigation={
        <Navigation
          activeHref={`/analytics/${projectId}/app/${appId}/realtime`}
        />
      }
    />
  );
};

export default AnalyticsRealtime;
