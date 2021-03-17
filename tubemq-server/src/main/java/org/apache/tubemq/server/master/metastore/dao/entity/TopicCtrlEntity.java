/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tubemq.server.master.metastore.dao.entity;

import java.util.Date;
import org.apache.tubemq.corebase.TBaseConstants;
import org.apache.tubemq.server.common.statusdef.EnableStatus;
import org.apache.tubemq.server.master.bdbstore.bdbentitys.BdbTopicAuthControlEntity;


/*
 * store the topic authenticate control setting
 *
 */
public class TopicCtrlEntity extends BaseEntity {

    private String topicName = "";
    private EnableStatus authCtrlStatus = EnableStatus.STATUS_UNDEFINE;
    private int maxMsgSizeInB = TBaseConstants.META_VALUE_UNDEFINED;


    public TopicCtrlEntity() {
        super();
    }

    public TopicCtrlEntity(String topicName, boolean enableAuth, int maxMsgSizeInB,
                           long dataVersionId, String createUser,
                           Date createDate, String modifyUser, Date modifyDate) {
        super(dataVersionId, createUser, createDate, modifyUser, modifyDate);
        this.topicName = topicName;
        this.maxMsgSizeInB = maxMsgSizeInB;
        if (enableAuth) {
            this.authCtrlStatus = EnableStatus.STATUS_ENABLE;
        } else {
            this.authCtrlStatus = EnableStatus.STATUS_DISABLE;
        }
    }

    public TopicCtrlEntity(BdbTopicAuthControlEntity bdbEntity) {
        super(bdbEntity.getDataVerId(),
                bdbEntity.getCreateUser(), bdbEntity.getCreateDate());
        this.topicName = bdbEntity.getTopicName();
        this.maxMsgSizeInB = bdbEntity.getMaxMsgSize();
        if (bdbEntity.isEnableAuthControl()) {
            this.authCtrlStatus = EnableStatus.STATUS_ENABLE;
        } else {
            this.authCtrlStatus = EnableStatus.STATUS_DISABLE;
        }
        this.setAttributes(bdbEntity.getAttributes());
    }

    public BdbTopicAuthControlEntity buildBdbTopicAuthControlEntity() {
        BdbTopicAuthControlEntity bdbEntity =
                new BdbTopicAuthControlEntity(topicName, isAuthCtrlEnable(),
                        getAttributes(), getCreateUser(), getCreateDate());
        bdbEntity.setDataVerId(getDataVersionId());
        bdbEntity.setMaxMsgSize(maxMsgSizeInB);
        return bdbEntity;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public boolean isAuthCtrlEnable() {
        return authCtrlStatus == EnableStatus.STATUS_ENABLE;
    }

    public void setAuthCtrlStatus(boolean enableAuth) {
        if (enableAuth) {
            this.authCtrlStatus = EnableStatus.STATUS_ENABLE;
        } else {
            this.authCtrlStatus = EnableStatus.STATUS_DISABLE;
        }
    }
}
