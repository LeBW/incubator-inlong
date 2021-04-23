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

package org.apache.tubemq.server.master.web.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.apache.tubemq.corebase.TBaseConstants;
import org.apache.tubemq.server.common.TServerConstants;
import org.apache.tubemq.server.common.fielddef.WebFieldDef;
import org.apache.tubemq.server.common.utils.ProcessResult;
import org.apache.tubemq.server.common.utils.WebParameterUtils;
import org.apache.tubemq.server.master.TMaster;
import org.apache.tubemq.server.master.metamanage.metastore.dao.entity.BaseEntity;
import org.apache.tubemq.server.master.metamanage.metastore.dao.entity.ClusterSettingEntity;
import org.apache.tubemq.server.master.metamanage.metastore.dao.entity.GroupResCtrlEntity;



public class WebGroupResCtrlHandler extends AbstractWebHandler {

    public WebGroupResCtrlHandler(TMaster master) {
        super(master);
    }

    @Override
    public void registerWebApiMethod() {
        // register query method
        registerQueryWebMethod("admin_query_group_resctrl_info",
                "adminQueryGroupResCtrlConf");
        // register modify method
        registerModifyWebMethod("admin_add_group_resctrl_info",
                "adminAddGroupResCtrlConf");
        registerModifyWebMethod("admin_batch_add_group_resctrl_info",
                "adminBatchAddGroupResCtrlConf");
        registerModifyWebMethod("admin_update_group_resctrl_info",
                "adminModGroupResCtrlConf");
        registerModifyWebMethod("admin_batch_update_group_resctrl_info",
                "adminBatchUpdGroupResCtrlConf");
        registerModifyWebMethod("admin_delete_group_resctrl_info",
                "adminDelGroupResCtrlConf");
    }

    /**
     * query group resource control info
     *
     * @param req
     * @return
     */
    public StringBuilder adminQueryGroupResCtrlConf(HttpServletRequest req) {
        ProcessResult result = new ProcessResult();
        StringBuilder sBuffer = new StringBuilder(512);
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
        Set<String> inGroupSet = (Set<String>) result.retData1;
        // get consumeEnable info
        if (!WebParameterUtils.getBooleanParamValue(req,
                WebFieldDef.CONSUMEENABLE, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Boolean consumeEnable = (Boolean) result.retData1;
        // get resCheckStatus info
        if (!WebParameterUtils.getBooleanParamValue(req,
                WebFieldDef.RESCHECKENABLE, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Boolean resCheckEnable = (Boolean) result.retData1;
        // get and valid qryPriorityId info
        if (!WebParameterUtils.getQryPriorityIdParameter(req,
                false, TBaseConstants.META_VALUE_UNDEFINED,
                TServerConstants.QRY_PRIORITY_MIN_VALUE, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        int inQryPriorityId = (int) result.retData1;
        // get flowCtrlEnable info
        if (!WebParameterUtils.getBooleanParamValue(req,
                WebFieldDef.FLOWCTRLENABLE, false, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Boolean flowCtrlEnable = (Boolean) result.retData1;
        entity.updModifyInfo(entity.getDataVerId(), consumeEnable, null,
                resCheckEnable, TBaseConstants.META_VALUE_UNDEFINED, inQryPriorityId,
                flowCtrlEnable, TBaseConstants.META_VALUE_UNDEFINED, null);
        Map<String, GroupResCtrlEntity> groupResCtrlEntityMap =
                metaDataManager.confGetGroupResCtrlConf(inGroupSet, entity);
        // build return result
        int totalCnt = 0;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuffer);
        for (GroupResCtrlEntity resCtrlEntity : groupResCtrlEntityMap.values()) {
            if (resCtrlEntity == null) {
                continue;
            }
            if (totalCnt++ > 0) {
                sBuffer.append(",");
            }
            sBuffer = entity.toWebJsonStr(sBuffer, true, true);
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuffer, totalCnt);
        return sBuffer;
    }

    /**
     * add group resource control info
     *
     * @param req
     * @return
     */
    public StringBuilder adminAddGroupResCtrlConf(HttpServletRequest req) {
        return innAddOrUpdGroupResCtrlConf(req, true);
    }

    /**
     * Add group resource control info in batch
     *
     * @param req
     * @return
     */
    private StringBuilder adminBatchAddGroupResCtrlConf(HttpServletRequest req) {
        return innBatchAddOrUpdGroupResCtrlConf(req, true);
    }

    /**
     * update group resource control info
     *
     * @param req
     * @return
     */
    public StringBuilder adminModGroupResCtrlConf(HttpServletRequest req) {
        return innAddOrUpdGroupResCtrlConf(req, false);
    }

    /**
     * update group resource control info in batch
     *
     * @param req
     * @return
     */
    private StringBuilder adminBatchUpdGroupResCtrlConf(HttpServletRequest req) {
        return innBatchAddOrUpdGroupResCtrlConf(req, false);
    }

    /**
     * delete group resource control rule
     *
     * @param req
     * @return
     */
    public StringBuilder adminDelGroupResCtrlConf(HttpServletRequest req) {
        ProcessResult result = new ProcessResult();
        StringBuilder sBuffer = new StringBuilder(512);
        // valid operation authorize info
        if (!WebParameterUtils.validReqAuthorizeInfo(req,
                WebFieldDef.ADMINAUTHTOKEN, true, master, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
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
        Set<String> batchGroupNames = (Set<String>) result.retData1;
        // delete group resource record
        List<GroupProcessResult> retInfo =
                metaDataManager.delGroupResCtrlConf(opEntity.getModifyUser(),
                        batchGroupNames, sBuffer, result);
        return buildRetInfo(retInfo, sBuffer);
    }

    private StringBuilder innAddOrUpdGroupResCtrlConf(HttpServletRequest req,
                                                      boolean isAddOp) {
        ProcessResult result = new ProcessResult();
        StringBuilder sBuffer = new StringBuilder(512);
        // valid operation authorize info
        if (!WebParameterUtils.validReqAuthorizeInfo(req,
                WebFieldDef.ADMINAUTHTOKEN, true, master, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
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
        Set<String> batchGroupNames = (Set<String>) result.retData1;
        // get consumeEnable info
        if (!WebParameterUtils.getBooleanParamValue(req, WebFieldDef.CONSUMEENABLE,
                false, (isAddOp ? true : null), sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Boolean consumeEnable = (Boolean) result.retData1;
        // get disableReason info
        if (!WebParameterUtils.getStringParamValue(req, WebFieldDef.REASON,
                false, (isAddOp ? "" : null), sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        String disableRsn = (String) result.retData1;
        // get resCheckStatus info
        if (!WebParameterUtils.getBooleanParamValue(req, WebFieldDef.RESCHECKENABLE,
                false, (isAddOp ? false : null), sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Boolean resCheckEnable = (Boolean) result.retData1;
        // get and valid allowedBrokerClientRate info
        if (!WebParameterUtils.getIntParamValue(req, WebFieldDef.ALWDBCRATE,
                false, (isAddOp ? TServerConstants.GROUP_BROKER_CLIENT_RATE_MIN
                        : TBaseConstants.META_VALUE_UNDEFINED),
                TServerConstants.GROUP_BROKER_CLIENT_RATE_MIN, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        int allowedBClientRate = (int) result.retData1;
        // get def cluster setting info
        ClusterSettingEntity defClusterSetting =
                metaDataManager.getClusterDefSetting(false);
        // get and valid qryPriorityId info
        if (!WebParameterUtils.getQryPriorityIdParameter(req,
                false, (isAddOp ? defClusterSetting.getQryPriorityId()
                        : TBaseConstants.META_VALUE_UNDEFINED),
                TServerConstants.QRY_PRIORITY_MIN_VALUE, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        int qryPriorityId = (int) result.retData1;
        // get flowCtrlEnable info
        if (!WebParameterUtils.getBooleanParamValue(req, WebFieldDef.FLOWCTRLENABLE,
                false, (isAddOp ? false : null), sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Boolean flowCtrlEnable = (Boolean) result.retData1;
        // get and flow control rule info
        int flowRuleCnt = WebParameterUtils.getAndCheckFlowRules(req,
                (isAddOp ? TServerConstants.BLANK_FLOWCTRL_RULES : null), sBuffer, result);
        if (!result.success) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        String flowCtrlInfo = (String) result.retData1;
        // add group resource record
        GroupProcessResult retItem;
        List<GroupProcessResult> retInfo = new ArrayList<>();
        for (String groupName : batchGroupNames) {
            retItem = metaDataManager.addOrUpdGroupResCtrlConf(isAddOp, opEntity, groupName,
                    consumeEnable, disableRsn, resCheckEnable, allowedBClientRate,
                    qryPriorityId, flowCtrlEnable, flowRuleCnt, flowCtrlInfo, sBuffer, result);
            retInfo.add(retItem);
        }
        return buildRetInfo(retInfo, sBuffer);
    }


    private StringBuilder innBatchAddOrUpdGroupResCtrlConf(HttpServletRequest req,
                                                           boolean isAddOp) {
        ProcessResult result = new ProcessResult();
        StringBuilder sBuffer = new StringBuilder(512);
        // valid operation authorize info
        if (!WebParameterUtils.validReqAuthorizeInfo(req,
                WebFieldDef.ADMINAUTHTOKEN, true, master, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        // check and get operation info
        if (!WebParameterUtils.getAUDBaseInfo(req, isAddOp, null, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        BaseEntity defOpEntity = (BaseEntity) result.getRetData();
        // get group resource control json record
        if (!getGroupResCtrlJsonSetInfo(req, isAddOp, defOpEntity, sBuffer, result)) {
            WebParameterUtils.buildFailResult(sBuffer, result.errInfo);
            return sBuffer;
        }
        Map<String, GroupResCtrlEntity> addRecordMap =
                (Map<String, GroupResCtrlEntity>) result.getRetData();
        // add or update group resource record
        List<GroupProcessResult> retInfo = new ArrayList<>();
        for (GroupResCtrlEntity newResCtrlEntity : addRecordMap.values()) {
            retInfo.add(metaDataManager.addOrUpdGroupResCtrlConf(
                    isAddOp, newResCtrlEntity, sBuffer, result));
        }
        return buildRetInfo(retInfo, sBuffer);
    }

    private boolean getGroupResCtrlJsonSetInfo(HttpServletRequest req, boolean isAddOp,
                                               BaseEntity defOpEntity, StringBuilder sBuffer,
                                               ProcessResult result) {
        if (!WebParameterUtils.getJsonArrayParamValue(req,
                WebFieldDef.GROUPRESCTRLSET, true, null, result)) {
            return result.success;
        }
        List<Map<String, String>> ctrlJsonArray =
                (List<Map<String, String>>) result.retData1;
        // get default qryPriorityId
        ClusterSettingEntity defClusterSetting =
                metaDataManager.getClusterDefSetting(false);
        int defQryPriorityId = defClusterSetting.getQryPriorityId();
        // check and get topic control configure
        GroupResCtrlEntity itemEntity;
        Map<String, String> itemValueMap;
        HashMap<String, GroupResCtrlEntity> addRecordMap = new HashMap<>();
        for (int j = 0; j < ctrlJsonArray.size(); j++) {
            itemValueMap = ctrlJsonArray.get(j);
            // check and get operation info
            if (!WebParameterUtils.getAUDBaseInfo(itemValueMap,
                    isAddOp, defOpEntity, sBuffer, result)) {
                return result.isSuccess();
            }
            BaseEntity itemOpEntity = (BaseEntity) result.getRetData();
            // get group configure info
            if (!WebParameterUtils.getStringParamValue(itemValueMap,
                    WebFieldDef.GROUPNAME, true, "", sBuffer, result)) {
                return result.success;
            }
            String groupName = (String) result.retData1;
            // get consumeEnable info
            if (!WebParameterUtils.getBooleanParamValue(itemValueMap, WebFieldDef.CONSUMEENABLE,
                    false, (isAddOp ? true : null), sBuffer, result)) {
                return result.isSuccess();
            }
            Boolean consumeEnable = (Boolean) result.retData1;
            // get disableReason info
            if (!WebParameterUtils.getStringParamValue(itemValueMap,
                    WebFieldDef.REASON, false, (isAddOp ? "" : null), sBuffer, result)) {
                return result.isSuccess();
            }
            String disableRsn = (String) result.retData1;
            // get resCheckStatus info
            if (!WebParameterUtils.getBooleanParamValue(itemValueMap, WebFieldDef.RESCHECKENABLE,
                    false, (isAddOp ? false : null), sBuffer, result)) {
                return result.isSuccess();
            }
            Boolean resCheckEnable = (Boolean) result.retData1;
            // get and valid allowedBrokerClientRate info
            if (!WebParameterUtils.getIntParamValue(itemValueMap, WebFieldDef.ALWDBCRATE,
                    false, (isAddOp ? TServerConstants.GROUP_BROKER_CLIENT_RATE_MIN
                            : TBaseConstants.META_VALUE_UNDEFINED),
                    TServerConstants.GROUP_BROKER_CLIENT_RATE_MIN, sBuffer, result)) {
                return result.isSuccess();
            }
            int allowedBClientRate = (int) result.retData1;
            // get def cluster setting info
            // get and valid qryPriorityId info
            if (!WebParameterUtils.getQryPriorityIdParameter(itemValueMap,
                    false, (isAddOp ? defClusterSetting.getQryPriorityId()
                            : TBaseConstants.META_VALUE_UNDEFINED),
                    TServerConstants.QRY_PRIORITY_MIN_VALUE, sBuffer, result)) {
                return result.isSuccess();
            }
            int qryPriorityId = (int) result.retData1;
            // get flowCtrlEnable info
            if (!WebParameterUtils.getBooleanParamValue(itemValueMap,
                    WebFieldDef.FLOWCTRLENABLE, false,
                    (isAddOp ? false : null), sBuffer, result)) {
                return result.isSuccess();
            }
            Boolean flowCtrlEnable = (Boolean) result.retData1;
            // get and flow control rule info
            int flowRuleCnt = WebParameterUtils.getAndCheckFlowRules(itemValueMap,
                    (isAddOp ? TServerConstants.BLANK_FLOWCTRL_RULES : null), sBuffer, result);
            if (!result.success) {
                return result.isSuccess();
            }
            String flowCtrlInfo = (String) result.retData1;
            itemEntity =
                    new GroupResCtrlEntity(itemOpEntity, groupName);
            itemEntity.updModifyInfo(itemEntity.getDataVerId(),
                    consumeEnable, disableRsn, resCheckEnable, allowedBClientRate,
                    qryPriorityId, flowCtrlEnable, flowRuleCnt, flowCtrlInfo);
            addRecordMap.put(itemEntity.getGroupName(), itemEntity);
        }
        // check result
        if (addRecordMap.isEmpty()) {
            result.setFailResult(sBuffer
                    .append("Not found record info in ")
                    .append(WebFieldDef.GROUPRESCTRLSET.name)
                    .append(" parameter!").toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        result.setSuccResult(addRecordMap);
        return result.isSuccess();
    }

    private StringBuilder buildRetInfo(List<GroupProcessResult> retInfo,
                                       StringBuilder sBuilder) {
        int totalCnt = 0;
        WebParameterUtils.buildSuccessWithDataRetBegin(sBuilder);
        for (GroupProcessResult entry : retInfo) {
            if (totalCnt++ > 0) {
                sBuilder.append(",");
            }
            sBuilder.append("{\"groupName\":\"").append(entry.getGroupName()).append("\"")
                    .append(",\"success\":").append(entry.isSuccess())
                    .append(",\"errCode\":").append(entry.getErrCode())
                    .append(",\"errInfo\":\"").append(entry.getErrInfo()).append("\"}");
        }
        WebParameterUtils.buildSuccessWithDataRetEnd(sBuilder, totalCnt);
        return sBuilder;
    }

}
