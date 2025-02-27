/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.master.web.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.server.common.TServerConstants;
import org.apache.inlong.tubemq.server.common.fielddef.WebFieldDef;
import org.apache.inlong.tubemq.server.common.utils.ProcessResult;
import org.apache.inlong.tubemq.server.common.utils.WebParameterUtils;
import org.apache.inlong.tubemq.server.master.TMaster;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BaseEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.GroupConsumeCtrlEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.GroupResCtrlEntity;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.ConsumerBandInfo;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.ConsumerInfoHolder;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.NodeRebInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Deprecated
public class WebAdminGroupCtrlHandler extends AbstractWebHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(WebAdminGroupCtrlHandler.class);


    public WebAdminGroupCtrlHandler(TMaster master) {
        super(master);
    }

    @Override
    public void registerWebApiMethod() {
        // register query method
        registerQueryWebMethod("admin_query_black_consumer_group_info",
                "adminQueryBlackGroupInfo");
        registerQueryWebMethod("admin_query_allowed_consumer_group_info",
                "adminQueryConsumerGroupInfo");
        registerQueryWebMethod("admin_query_group_filtercond_info",
                "adminQueryGroupFilterCondInfo");
        registerQueryWebMethod("admin_query_consume_group_setting",
                "adminQueryConsumeGroupSetting");
        // register modify method
        registerModifyWebMethod("admin_add_black_consumergroup_info",
                "adminAddBlackGroupInfo");
        registerModifyWebMethod("admin_bath_add_black_consumergroup_info",
                "adminBatchAddBlackGroupInfo");
        registerModifyWebMethod("admin_delete_black_consumergroup_info",
                "adminDeleteBlackGroupInfo");
        registerModifyWebMethod("admin_add_authorized_consumergroup_info",
                "adminAddConsumerGroupInfo");
        registerModifyWebMethod("admin_delete_allowed_consumer_group_info",
                "adminDeleteConsumerGroupInfo");
        registerModifyWebMethod("admin_bath_add_authorized_consumergroup_info",
                "adminBatchAddConsumerGroupInfo");
        registerModifyWebMethod("admin_add_group_filtercond_info",
                "adminAddGroupFilterCondInfo");
        registerModifyWebMethod("admin_bath_add_group_filtercond_info",
                "adminBatchAddGroupFilterCondInfo");
        registerModifyWebMethod("admin_mod_group_filtercond_info",
                "adminModGroupFilterCondInfo");
        registerModifyWebMethod("admin_bath_mod_group_filtercond_info",
                "adminBatchModGroupFilterCondInfo");
        registerModifyWebMethod("admin_del_group_filtercond_info",
                "adminDeleteGroupFilterCondInfo");
        registerModifyWebMethod("admin_add_consume_group_setting",
                "adminAddConsumeGroupSettingInfo");
        registerModifyWebMethod("admin_bath_add_consume_group_setting",
                "adminBatchAddConsumeGroupSetting");
        registerModifyWebMethod("admin_upd_consume_group_setting",
                "adminUpdConsumeGroupSetting");
        registerModifyWebMethod("admin_del_consume_group_setting",
                "adminDeleteConsumeGroupSetting");
        registerModifyWebMethod("admin_rebalance_group_allocate",
                "adminRebalanceGroupAllocateInfo");
    }

    /**
     * Query black consumer group info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminQueryBlackGroupInfo(HttpServletRequest req,
                                                  StringBuilder sBuffer,
                                                  ProcessResult result) {
        // build query entity
        GroupConsumeCtrlEntity entity = new GroupConsumeCtrlEntity();
        // get queried operation info, for createUser, modifyUser, dataVersionId
        if (!WebParameterUtils.getQueriedOperateInfo(req, entity, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> groupNameSet = (Set<String>) result.getRetData();
        // check and get topicName field
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSTOPICNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        // only query disable consume group
        entity.setConsumeEnable(false);
        Map<String, List<GroupConsumeCtrlEntity>> qryResult =
                metaDataManager.getGroupConsumeCtrlConf(groupNameSet, topicNameSet, entity);
        int totalCnt = 0;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (List<GroupConsumeCtrlEntity> entryList : qryResult.values()) {
            for (GroupConsumeCtrlEntity entry : entryList) {
                if (totalCnt++ > 0) {
                    sBuffer.append(",");
                }
                sBuffer.append("{\"groupName\":\"").append(entry.getGroupName()).append("\"")
                        .append(",\"reason\":\"").append(entry.getDisableReason()).append("\"")
                        .append(",\"dataVersionId\":").append(entry.getDataVerId())
                        .append(",\"createUser\":\"").append(entry.getCreateUser()).append("\"")
                        .append(",\"createDate\":\"").append(entry.getCreateDateStr()).append("\"")
                        .append(",\"modifyUser\":\"").append(entry.getModifyUser()).append("\"")
                        .append(",\"modifyDate\":\"").append(entry.getModifyDateStr()).append("\"}");
            }
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    /**
     * Query allowed(authorized?) consumer group info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminQueryConsumerGroupInfo(HttpServletRequest req,
                                                     StringBuilder sBuffer,
                                                     ProcessResult result) {
        // build query entity
        GroupConsumeCtrlEntity qryEntity = new GroupConsumeCtrlEntity();
        // get queried operation info, for createUser, modifyUser, dataVersionId
        if (!WebParameterUtils.getQueriedOperateInfo(req, qryEntity, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> groupNameSet = (Set<String>) result.getRetData();
        // check and get topicName field
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSTOPICNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        qryEntity.setConsumeEnable(true);
        Map<String, List<GroupConsumeCtrlEntity>> qryResultMap =
                metaDataManager.getGroupConsumeCtrlConf(groupNameSet, topicNameSet, qryEntity);
        int totalCnt = 0;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (List<GroupConsumeCtrlEntity> entryLst : qryResultMap.values()) {
            if (entryLst == null || entryLst.isEmpty()) {
                continue;
            }
            for (GroupConsumeCtrlEntity entry : entryLst) {
                if (entry == null) {
                    continue;
                }
                if (totalCnt++ > 0) {
                    sBuffer.append(",");
                }
                sBuffer.append("{\"topicName\":\"").append(entry.getTopicName())
                        .append("\",\"groupName\":\"").append(entry.getGroupName())
                        .append("\",\"dataVersionId\":").append(entry.getDataVerId())
                        .append(",\"createUser\":\"").append(entry.getCreateUser())
                        .append("\",\"createDate\":\"").append(entry.getCreateDateStr())
                        .append("\",\"modifyUser\":\"").append(entry.getModifyUser())
                        .append("\",\"modifyDate\":\"").append(entry.getModifyDateStr()).append("\"}");
            }
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    /**
     * Query group filter condition info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminQueryGroupFilterCondInfo(HttpServletRequest req,
                                                       StringBuilder sBuffer,
                                                       ProcessResult result) {
        // build query entity
        GroupConsumeCtrlEntity qryEntity = new GroupConsumeCtrlEntity();
        // get queried operation info, for createUser, modifyUser, dataVersionId
        if (!WebParameterUtils.getQueriedOperateInfo(req, qryEntity, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        final Set<String> groupNameSet = (Set<String>) result.getRetData();
        // check and get topicName field
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSTOPICNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        final Set<String> topicNameSet = (Set<String>) result.getRetData();
        // check and get condStatus field
        if (!getCondStatusParamValue(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Boolean filterEnable = (Boolean) result.getRetData();
        // get filterConds info
        if (!WebParameterUtils.getFilterCondSet(req, false, true, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> filterCondSet = (Set<String>) result.getRetData();
        qryEntity.updModifyInfo(qryEntity.getDataVerId(),
                null, null, filterEnable, null);
        Map<String, List<GroupConsumeCtrlEntity>> qryResultMap =
                metaDataManager.getGroupConsumeCtrlConf(groupNameSet, topicNameSet, qryEntity);
        // build return result
        int totalCnt = 0;
        int condStatusId = 0;
        String itemFilterStr;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (List<GroupConsumeCtrlEntity> consumeCtrlEntityList : qryResultMap.values()) {
            if (consumeCtrlEntityList == null || consumeCtrlEntityList.isEmpty()) {
                continue;
            }
            for (GroupConsumeCtrlEntity entry : consumeCtrlEntityList) {
                if (entry == null
                        || !WebParameterUtils.isFilterSetFullIncluded(
                                filterCondSet, entry.getFilterCondStr())) {
                    continue;
                }
                if (totalCnt++ > 0) {
                    sBuffer.append(",");
                }
                condStatusId = entry.getFilterEnable().isEnable() ? 2 : 0;
                itemFilterStr = (entry.getFilterCondStr().length() <= 2)
                        ? "" : entry.getFilterCondStr();
                sBuffer.append("{\"topicName\":\"").append(entry.getTopicName())
                        .append("\",\"groupName\":\"").append(entry.getGroupName())
                        .append("\",\"condStatus\":").append(condStatusId)
                        .append(",\"filterConds\":\"").append(itemFilterStr)
                        .append("\",\"dataVersionId\":").append(entry.getDataVerId())
                        .append(",\"createUser\":\"").append(entry.getCreateUser())
                        .append("\",\"createDate\":\"").append(entry.getCreateDateStr())
                        .append("\",\"modifyUser\":\"").append(entry.getModifyUser())
                        .append("\",\"modifyDate\":\"").append(entry.getModifyDateStr()).append("\"}");
            }
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    /**
     * Query consumer group setting
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminQueryConsumeGroupSetting(HttpServletRequest req,
                                                       StringBuilder sBuffer,
                                                       ProcessResult result) {
        // build query entity
        GroupResCtrlEntity entity = new GroupResCtrlEntity();
        // get queried operation info, for createUser, modifyUser, dataVersionId
        if (!WebParameterUtils.getQueriedOperateInfo(req, entity, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> groupNameSet = (Set<String>) result.getRetData();
        // get group list
        if (!WebParameterUtils.getIntParamValue(req,
                WebFieldDef.OLDALWDBCRATE, false,
                TBaseConstants.META_VALUE_UNDEFINED, 0, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        int allowedBClientRate = (int) result.getRetData();
        // query matched records
        entity.updModifyInfo(entity.getDataVerId(), null, allowedBClientRate,
                TBaseConstants.META_VALUE_UNDEFINED, null,
                TBaseConstants.META_VALUE_UNDEFINED, null);
        Map<String, GroupResCtrlEntity> groupResCtrlEntityMap =
                metaDataManager.confGetGroupResCtrlConf(groupNameSet, entity);
        // build return result
        int totalCnt = 0;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (GroupResCtrlEntity entry : groupResCtrlEntityMap.values()) {
            if (entry == null) {
                continue;
            }
            if (totalCnt++ > 0) {
                sBuffer.append(",");
            }
            sBuffer.append("{\"groupName\":\"").append(entry.getGroupName())
                    .append("\",\"enableBind\":1,\"allowedBClientRate\":")
                    .append(entry.getAllowedBrokerClientRate())
                    .append(",\"attributes\":\"\",\"lastBindUsedDate\":\"-\"")
                    .append("\",\"dataVersionId\":").append(entry.getDataVerId())
                    .append(",\"createUser\":\"").append(entry.getCreateUser())
                    .append("\",\"createDate\":\"").append(entry.getCreateDateStr())
                    .append("\",\"modifyUser\":\"").append(entry.getModifyUser())
                    .append("\",\"modifyDate\":\"").append(entry.getModifyDateStr()).append("\"}");
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    /**
     * Add black consumer group info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminAddBlackGroupInfo(HttpServletRequest req,
                                                StringBuilder sBuffer,
                                                ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> groupNameSet = (Set<String>) result.getRetData();
        // check and get topicName field
        if (!WebParameterUtils.getAndValidTopicNameInfo(req,
                metaDataManager, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        // add black list records
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        for (String groupName : groupNameSet) {
            for (String topicName : topicNameSet) {
                retInfoList.add(metaDataManager.addOrUpdGroupConsumeCtrlInfo(opEntity, groupName,
                        topicName, Boolean.FALSE, "Old API Set", null, null, sBuffer, result));
            }
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    /**
     * Add black consumer group info in batch
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminBatchAddBlackGroupInfo(HttpServletRequest req,
                                                     StringBuilder sBuffer,
                                                     ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // check and get groupNameJsonSet info
        if (!getGroupCsmJsonSetInfo(req, opEntity, Boolean.FALSE, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Map<String, GroupConsumeCtrlEntity> addRecordMap =
                (Map<String, GroupConsumeCtrlEntity>) result.getRetData();
        // add or update and buid result
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        for (GroupConsumeCtrlEntity entry : addRecordMap.values()) {
            retInfoList.add(metaDataManager.addOrUpdGroupConsumeCtrlInfo(entry, sBuffer, result));
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    /**
     * Delete black consumer group info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminDeleteBlackGroupInfo(HttpServletRequest req,
                                                   StringBuilder sBuffer,
                                                   ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> groupNameSet = (Set<String>) result.getRetData();
        // check and get topicName field
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSTOPICNAME, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        // add allowed consume records
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        for (String groupName : groupNameSet) {
            for (String topicName : topicNameSet) {
                retInfoList.add(metaDataManager.addOrUpdGroupConsumeCtrlInfo(opEntity, groupName,
                        topicName, Boolean.TRUE, "enable consume", null, null, sBuffer, result));

            }
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    /**
     * Add authorized consumer group info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminAddConsumerGroupInfo(HttpServletRequest req,
                                                   StringBuilder sBuffer,
                                                   ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> groupNameSet = (Set<String>) result.getRetData();
        // check and get topicName field
        if (!WebParameterUtils.getAndValidTopicNameInfo(req,
                metaDataManager, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        // add allowed consume records
        for (String groupName : groupNameSet) {
            for (String topicName : topicNameSet) {
                retInfoList.add(metaDataManager.addOrUpdGroupConsumeCtrlInfo(opEntity, groupName,
                        topicName, Boolean.TRUE, "enable consume", null, null, sBuffer, result));
            }
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    /**
     * Add authorized consumer group info in batch
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminBatchAddConsumerGroupInfo(HttpServletRequest req,
                                                        StringBuilder sBuffer,
                                                        ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // check and get groupNameJsonSet info
        if (!getGroupCsmJsonSetInfo(req, opEntity, Boolean.TRUE, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Map<String, GroupConsumeCtrlEntity> addRecordMap =
                (Map<String, GroupConsumeCtrlEntity>) result.getRetData();
        // add or update and buid result
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        for (GroupConsumeCtrlEntity entry : addRecordMap.values()) {
            retInfoList.add(metaDataManager.addOrUpdGroupConsumeCtrlInfo(entry, sBuffer, result));
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    /**
     * Delete allowed(authorized) consumer group info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminDeleteConsumerGroupInfo(HttpServletRequest req,
                                                      StringBuilder sBuffer,
                                                      ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> groupNameSet = (Set<String>) result.getRetData();
        // check and get topicName field
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSTOPICNAME, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        List<GroupProcessResult> retInfoList =
                metaDataManager.delGroupConsumeCtrlConf(opEntity.getModifyUser(),
                        groupNameSet, topicNameSet, sBuffer, result);
        return buildRetInfo(retInfoList, sBuffer);
    }

    /**
     * Add group filter condition info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminAddGroupFilterCondInfo(HttpServletRequest req,
                                                     StringBuilder sBuffer,
                                                     ProcessResult result) {
        return innAddOrModGroupFilterCondInfo(req, sBuffer, result, true);
    }

    /**
     * Modify group filter condition info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminModGroupFilterCondInfo(HttpServletRequest req,
                                                     StringBuilder sBuffer,
                                                     ProcessResult result) {
        return innAddOrModGroupFilterCondInfo(req, sBuffer, result, false);
    }

    /**
     * Add group filter info in batch
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminBatchAddGroupFilterCondInfo(HttpServletRequest req,
                                                          StringBuilder sBuffer,
                                                          ProcessResult result) {
        return innBatchAddOrUpdGroupFilterCondInfo(req, sBuffer, result, true);
    }

    /**
     * Modify group filter condition info in batch
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminBatchModGroupFilterCondInfo(HttpServletRequest req,
                                                          StringBuilder sBuffer,
                                                          ProcessResult result) {
        return innBatchAddOrUpdGroupFilterCondInfo(req, sBuffer, result, false);
    }

    /**
     * Delete group filter condition info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminDeleteGroupFilterCondInfo(HttpServletRequest req,
                                                        StringBuilder sBuffer,
                                                        ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> groupNameSet = (Set<String>) result.getRetData();
        // check and get topicName field
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSTOPICNAME, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        for (String groupName : groupNameSet) {
            for (String topicName : topicNameSet) {
                retInfoList.add(metaDataManager.addOrUpdGroupConsumeCtrlInfo(opEntity,
                        groupName, topicName, Boolean.TRUE, "enable consume",
                        false, TServerConstants.BLANK_FILTER_ITEM_STR, sBuffer, result));
            }
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    /**
     * Re-balance group allocation info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminRebalanceGroupAllocateInfo(HttpServletRequest req,
                                                         StringBuilder sBuffer,
                                                         ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        final BaseEntity opEntity = (BaseEntity) result.getRetData();
        // get group configure info
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.GROUPNAME, true, "", sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        String groupName = (String) result.getRetData();
        // get reJoinWait info
        if (!WebParameterUtils.getIntParamValue(req,
                WebFieldDef.REJOINWAIT, false, 0, 0, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        final int reJoinWait = (int) result.getRetData();
        // get consumerId list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSCONSUMERID, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.getErrInfo());
            return sBuffer;
        }
        Set<String> consumerIdSet = (Set<String>) result.getRetData();
        ConsumerInfoHolder consumerInfoHolder =
                master.getConsumerHolder();
        ConsumerBandInfo consumerBandInfo =
                consumerInfoHolder.getConsumerBandInfo(groupName);
        if (consumerBandInfo == null) {
            String errInfo = sBuffer.append("The group(")
                    .append(groupName).append(") not online!").toString();
            sBuffer.delete(0, sBuffer.length());
            WebParameterUtils.buildFailResult(sBuffer, errInfo);
            return sBuffer;
        }
        Map<String, NodeRebInfo> nodeRebInfoMap = consumerBandInfo.getRebalanceMap();
        for (String consumerId : consumerIdSet) {
            if (nodeRebInfoMap.containsKey(consumerId)) {
                String errInfo = sBuffer.append("Duplicated set for consumerId(")
                        .append(consumerId).append(") in group(")
                        .append(groupName).append(")! \"}").toString();
                sBuffer.delete(0, sBuffer.length());
                WebParameterUtils.buildFailResult(sBuffer, errInfo);
                return sBuffer;
            }
        }
        logger.info(sBuffer.append("[Re-balance] Add rebalance consumer: group=")
                .append(groupName).append(", consumerIds=")
                .append(consumerIdSet.toString())
                .append(", reJoinWait=").append(reJoinWait)
                .append(", creator=").append(opEntity.getModifyUser()).toString());
        sBuffer.delete(0, sBuffer.length());
        consumerInfoHolder.addRebConsumerInfo(groupName, consumerIdSet, reJoinWait);
        WebParameterUtils.buildSuccessResult(sBuffer);
        return sBuffer;
    }

    /**
     * Add consumer group setting
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminAddConsumeGroupSettingInfo(HttpServletRequest req,
                                                         StringBuilder sBuffer,
                                                         ProcessResult result) {
        return innAddOrUpdConsumeGroupSettingInfo(req, sBuffer, result, true);
    }

    /**
     * Update consumer group setting
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminUpdConsumeGroupSetting(HttpServletRequest req,
                                                     StringBuilder sBuffer,
                                                     ProcessResult result) {
        return innAddOrUpdConsumeGroupSettingInfo(req, sBuffer, result, false);
    }

    /**
     * Add consumer group setting in batch
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminBatchAddConsumeGroupSetting(HttpServletRequest req,
                                                          StringBuilder sBuffer,
                                                          ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // check and get groupNameJsonSet info
        if (!getGroupCtrlJsonSetInfo(req, opEntity, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Map<String, GroupResCtrlEntity> addRecordMap =
                (Map<String, GroupResCtrlEntity>) result.getRetData();
        // add or update and build result
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        for (GroupResCtrlEntity resCtrlEntity : addRecordMap.values()) {
            retInfoList.add(metaDataManager.addOrUpdGroupResCtrlConf(
                    resCtrlEntity, sBuffer, result));
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    /**
     * Delete consumer group setting
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @return    process result
     */
    public StringBuilder adminDeleteConsumeGroupSetting(HttpServletRequest req,
                                                        StringBuilder sBuffer,
                                                        ProcessResult result) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> groupNameSet = (Set<String>) result.getRetData();
        // add or update group control record
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        for (String groupName : groupNameSet) {
            retInfoList.add(metaDataManager.addOrUpdGroupResCtrlConf(false, opEntity,
                    groupName, Boolean.FALSE, 0,
                    TBaseConstants.META_VALUE_UNDEFINED, null,
                    TBaseConstants.META_VALUE_UNDEFINED, null, sBuffer, result));
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    private StringBuilder buildRetInfo(List<GroupProcessResult> retInfo,
                                       StringBuilder sBuffer) {
        int totalCnt = 0;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (GroupProcessResult entry : retInfo) {
            if (totalCnt++ > 0) {
                sBuffer.append(",");
            }
            sBuffer.append("{\"groupName\":\"").append(entry.getGroupName())
                    .append("\",\"success\":").append(entry.isSuccess())
                    .append(",\"errCode\":").append(entry.getErrCode())
                    .append(",\"errInfo\":\"").append(entry.getErrInfo()).append("\"}");
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    /**
     * Inner method: add consumer group setting
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @param isAddOp   whether add operation
     * @return    process result
     */
    private StringBuilder innAddOrUpdConsumeGroupSettingInfo(HttpServletRequest req,
                                                             StringBuilder sBuffer,
                                                             ProcessResult result,
                                                             boolean isAddOp) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, isAddOp, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // get group info
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> groupNameSet = (Set<String>) result.getRetData();
        // get resCheckStatus info
        if (!WebParameterUtils.getBooleanParamValue(req, WebFieldDef.RESCHECKENABLE,
                false, (isAddOp ? false : null), sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Boolean resChkEnable = (Boolean) result.getRetData();
        // get and valid allowedBClientRate info
        if (!WebParameterUtils.getIntParamValue(req, WebFieldDef.OLDALWDBCRATE,
                false, (isAddOp ? TServerConstants.GROUP_BROKER_CLIENT_RATE_MIN
                        : TBaseConstants.META_VALUE_UNDEFINED),
                TServerConstants.GROUP_BROKER_CLIENT_RATE_MIN, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        int allowedBClientRate = (int) result.getRetData();
        // add or update group control record
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        for (String groupName : groupNameSet) {
            if (isAddOp) {
                retInfoList.add(metaDataManager.addOrUpdGroupResCtrlConf(opEntity,
                        groupName, resChkEnable, allowedBClientRate, sBuffer, result));
            } else {
                retInfoList.add(metaDataManager.addOrUpdGroupResCtrlConf(isAddOp, opEntity,
                        groupName, resChkEnable, allowedBClientRate,
                        TBaseConstants.META_VALUE_UNDEFINED, null,
                        TBaseConstants.META_VALUE_UNDEFINED, null, sBuffer, result));
            }
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    /**
     * Inner method: modify group filter condition info
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @param isAddOp   whether add operation
     * @return    process result
     */
    private StringBuilder innAddOrModGroupFilterCondInfo(HttpServletRequest req,
                                                         StringBuilder sBuffer,
                                                         ProcessResult result,
                                                         boolean isAddOp) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, isAddOp, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // get group list
        if (!WebParameterUtils.getStringParamValue(req,
                WebFieldDef.COMPSGROUPNAME, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        final Set<String> groupNameSet = (Set<String>) result.getRetData();
        // check and get topicName field
        if (!WebParameterUtils.getAndValidTopicNameInfo(req,
                metaDataManager, true, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Set<String> topicNameSet = (Set<String>) result.getRetData();
        // check and get condStatus field
        if (!getCondStatusParamValue(req, false, (isAddOp ? false : null), sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Boolean filterEnable = (Boolean) result.getRetData();
        // get filterConds info
        if (!WebParameterUtils.getFilterCondString(req, false, isAddOp, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        String filterCondStr = (String) result.getRetData();
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        // modify filter consume records
        for (String groupName : groupNameSet) {
            for (String topicName : topicNameSet) {
                if (isAddOp) {
                    retInfoList.add(metaDataManager.addOrUpdGroupConsumeCtrlInfo(opEntity,
                            groupName, topicName, Boolean.TRUE, "enable consume",
                            filterEnable, filterCondStr, sBuffer, result));
                } else {
                    retInfoList.add(metaDataManager.addOrUpdGroupConsumeCtrlInfo(isAddOp,
                            opEntity, groupName, topicName, Boolean.TRUE, "enable consume",
                            filterEnable, filterCondStr, sBuffer, result));
                }
            }
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    /**
     * Inner method: add group filter info in batch
     *
     * @param req       Http Servlet Request
     * @param sBuffer   string buffer
     * @param result    process result
     * @param isAddOp   whether add operation
     * @return    process result
     */
    private StringBuilder innBatchAddOrUpdGroupFilterCondInfo(HttpServletRequest req,
                                                              StringBuilder sBuffer,
                                                              ProcessResult result,
                                                              boolean isAddOp) {
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, isAddOp, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        BaseEntity opEntity = (BaseEntity) result.getRetData();
        // check and get filterCondJsonSet info
        if (!getFilterJsonSetInfo(req, isAddOp, opEntity, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Map<String, GroupConsumeCtrlEntity> addRecordMap =
                (Map<String, GroupConsumeCtrlEntity>) result.getRetData();
        // add or update and build result
        List<GroupProcessResult> retInfoList = new ArrayList<>();
        for (GroupConsumeCtrlEntity entry : addRecordMap.values()) {
            if (isAddOp) {
                retInfoList.add(metaDataManager.addOrUpdGroupConsumeCtrlInfo(
                        entry, sBuffer, result));
            } else {
                retInfoList.add(metaDataManager.addOrUpdGroupConsumeCtrlInfo(
                        isAddOp, entry, sBuffer, result));
            }
        }
        return buildRetInfo(retInfoList, sBuffer);
    }

    private boolean getFilterJsonSetInfo(HttpServletRequest req, boolean isAddOp,
                                         BaseEntity defOpEntity, StringBuilder sBuffer,
                                         ProcessResult result) {
        if (!WebParameterUtils.getJsonArrayParamValue(req,
                WebFieldDef.FILTERJSONSET, true, null, result)) {
            return result.isSuccess();
        }
        List<Map<String, String>> groupJsonArray =
                (List<Map<String, String>>) result.getRetData();
        GroupConsumeCtrlEntity itemEntity;
        Map<String, GroupConsumeCtrlEntity> addRecordMap = new HashMap<>();
        Set<String> configuredTopicSet =
                metaDataManager.getTotalConfiguredTopicNames();
        for (Map<String, String> itemValueMap : groupJsonArray) {
            // check and get operation info
            if (!WebParameterUtils.getAUDBaseInfo(itemValueMap,
                    isAddOp, defOpEntity, sBuffer, result)) {
                return result.isSuccess();
            }
            final BaseEntity itemOpEntity = (BaseEntity) result.getRetData();
            // get group configure info
            if (!WebParameterUtils.getStringParamValue(itemValueMap,
                    WebFieldDef.GROUPNAME, true, "", sBuffer, result)) {
                return result.isSuccess();
            }
            final String groupName = (String) result.getRetData();
            if (!WebParameterUtils.getStringParamValue(itemValueMap,
                    WebFieldDef.TOPICNAME, true, "", sBuffer, result)) {
                return result.isSuccess();
            }
            String topicName = (String) result.getRetData();
            if (!configuredTopicSet.contains(topicName)) {
                result.setFailResult(sBuffer
                        .append(WebFieldDef.TOPICNAME.name)
                        .append(" ").append(topicName)
                        .append(" is not configure, please configure first!").toString());
                sBuffer.delete(0, sBuffer.length());
                return result.isSuccess();
            }
            // check and get condStatus field
            if (!getCondStatusParamValue(req, false,
                    (isAddOp ? false : null), sBuffer, result)) {
                return result.isSuccess();
            }
            Boolean filterEnable = (Boolean) result.getRetData();
            // get filterConds info
            if (!WebParameterUtils.getFilterCondString(req,
                    false, isAddOp, sBuffer, result)) {
                return result.isSuccess();
            }
            String filterCondStr = (String) result.getRetData();
            itemEntity =
                    new GroupConsumeCtrlEntity(itemOpEntity, groupName, topicName);
            itemEntity.updModifyInfo(itemOpEntity.getDataVerId(),
                    true, "enable consume", filterEnable, filterCondStr);
            addRecordMap.put(itemEntity.getGroupName(), itemEntity);
        }
        // check result
        if (addRecordMap.isEmpty()) {
            result.setFailResult(sBuffer
                    .append("Not found record info in ")
                    .append(WebFieldDef.FILTERJSONSET.name)
                    .append(" parameter!").toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        result.setSuccResult(addRecordMap);
        return result.isSuccess();
    }

    private boolean getGroupCtrlJsonSetInfo(HttpServletRequest req, BaseEntity defOpEntity,
                                            StringBuilder sBuffer, ProcessResult result) {
        if (!WebParameterUtils.getJsonArrayParamValue(req,
                WebFieldDef.GROUPJSONSET, true, null, result)) {
            return result.isSuccess();
        }
        List<Map<String, String>> groupJsonArray =
                (List<Map<String, String>>) result.getRetData();
        GroupResCtrlEntity itemEntity;
        Map<String, String> itemValueMap;
        Map<String, GroupResCtrlEntity> addRecordMap = new HashMap<>();
        Set<String> configuredTopicSet =
                metaDataManager.getTotalConfiguredTopicNames();
        for (int j = 0; j < groupJsonArray.size(); j++) {
            itemValueMap = groupJsonArray.get(j);
            // check and get operation info
            if (!WebParameterUtils.getAUDBaseInfo(itemValueMap,
                    true, defOpEntity, sBuffer, result)) {
                return result.isSuccess();
            }
            final BaseEntity itemOpEntity = (BaseEntity) result.getRetData();
            // get group configure info
            if (!WebParameterUtils.getStringParamValue(itemValueMap,
                    WebFieldDef.GROUPNAME, true, "", sBuffer, result)) {
                return result.isSuccess();
            }
            String groupName = (String) result.getRetData();
            // get resCheckStatus info
            if (!WebParameterUtils.getBooleanParamValue(itemValueMap, WebFieldDef.RESCHECKENABLE,
                    false, false, sBuffer, result)) {
                return result.isSuccess();
            }
            Boolean resChkEnable = (Boolean) result.getRetData();
            // get and valid allowedBClientRate info
            if (!WebParameterUtils.getIntParamValue(req, WebFieldDef.OLDALWDBCRATE,
                    false, TServerConstants.GROUP_BROKER_CLIENT_RATE_MIN,
                    TServerConstants.GROUP_BROKER_CLIENT_RATE_MIN, sBuffer, result)) {
                return result.isSuccess();
            }
            int allowedB2CRate = (int) result.getRetData();
            itemEntity =
                    new GroupResCtrlEntity(itemOpEntity, groupName);
            itemEntity.updModifyInfo(itemOpEntity.getDataVerId(), resChkEnable, allowedB2CRate,
                    TBaseConstants.META_VALUE_UNDEFINED, null,
                    TBaseConstants.META_VALUE_UNDEFINED, null);
            addRecordMap.put(itemEntity.getGroupName(), itemEntity);
        }
        // check result
        if (addRecordMap.isEmpty()) {
            result.setFailResult(sBuffer
                    .append("Not found record info in ")
                    .append(WebFieldDef.GROUPJSONSET.name)
                    .append(" parameter!").toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        result.setSuccResult(addRecordMap);
        return result.isSuccess();
    }

    private boolean getGroupCsmJsonSetInfo(HttpServletRequest req, BaseEntity defOpEntity,
                                           Boolean enableCsm, StringBuilder sBuffer,
                                           ProcessResult result) {
        if (!WebParameterUtils.getJsonArrayParamValue(req,
                WebFieldDef.GROUPJSONSET, true, null, result)) {
            return result.isSuccess();
        }
        List<Map<String, String>> groupJsonArray =
                (List<Map<String, String>>) result.getRetData();
        GroupConsumeCtrlEntity itemEntity;
        Map<String, GroupConsumeCtrlEntity> addRecordMap = new HashMap<>();
        Set<String> configuredTopicSet =
                metaDataManager.getTotalConfiguredTopicNames();
        for (Map<String, String> itemValueMap : groupJsonArray) {
            // check and get operation info
            if (!WebParameterUtils.getAUDBaseInfo(itemValueMap,
                    true, defOpEntity, sBuffer, result)) {
                return result.isSuccess();
            }
            final BaseEntity itemOpEntity = (BaseEntity) result.getRetData();
            // get group configure info
            if (!WebParameterUtils.getStringParamValue(itemValueMap,
                    WebFieldDef.GROUPNAME, true, "", sBuffer, result)) {
                return result.isSuccess();
            }
            String groupName = (String) result.getRetData();
            if (!WebParameterUtils.getStringParamValue(itemValueMap,
                    WebFieldDef.TOPICNAME, true, "", sBuffer, result)) {
                return result.isSuccess();
            }
            String topicName = (String) result.getRetData();
            if (!configuredTopicSet.contains(topicName)) {
                result.setFailResult(sBuffer
                        .append(WebFieldDef.TOPICNAME.name)
                        .append(" ").append(topicName)
                        .append(" is not configure, please configure first!").toString());
                sBuffer.delete(0, sBuffer.length());
                return result.isSuccess();
            }
            itemEntity =
                    new GroupConsumeCtrlEntity(itemOpEntity, groupName, topicName);
            itemEntity.updModifyInfo(itemOpEntity.getDataVerId(),
                    enableCsm, "Old API batch set", null, null);
            addRecordMap.put(itemEntity.getGroupName(), itemEntity);
        }
        // check result
        if (addRecordMap.isEmpty()) {
            result.setFailResult(sBuffer
                    .append("Not found record info in ")
                    .append(WebFieldDef.GROUPJSONSET.name)
                    .append(" parameter!").toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        result.setSuccResult(addRecordMap);
        return result.isSuccess();
    }

    private <T> boolean getCondStatusParamValue(T paramCntr, boolean required, Boolean defValue,
                                                StringBuilder sBuffer, ProcessResult result) {
        // check and get condStatus field
        if (!WebParameterUtils.getIntParamValue(paramCntr, WebFieldDef.CONDSTATUS,
                required, TBaseConstants.META_VALUE_UNDEFINED, 0, 2, sBuffer, result)) {
            return result.isSuccess();
        }
        int paramValue = (int) result.getRetData();
        if (paramValue == TBaseConstants.META_VALUE_UNDEFINED) {
            result.setSuccResult(defValue);
        } else {
            if (paramValue == 2) {
                result.setSuccResult(Boolean.TRUE);
            } else {
                result.setSuccResult(Boolean.FALSE);
            }
        }
        return result.isSuccess();
    }

}
