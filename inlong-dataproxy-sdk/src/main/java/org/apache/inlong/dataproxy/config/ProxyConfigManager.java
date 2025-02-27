/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.dataproxy.config;

import static org.apache.inlong.dataproxy.ConfigConstants.REQUEST_HEADER_AUTHORIZATION;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.net.ssl.SSLContext;

import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.apache.inlong.dataproxy.ProxyClientConfig;
import org.apache.inlong.dataproxy.ConfigConstants;
import org.apache.inlong.dataproxy.network.ClientMgr;
import org.apache.inlong.dataproxy.network.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProxyConfigManager extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(ProxyConfigManager.class);
    public static final String APPLICATION_JSON = "application/json";

    private List<HostInfo> proxyInfoList = new ArrayList<HostInfo>();
    /*the status of the cluster.if this value is changed,we need rechoose  three proxy*/
    private int oldStat = 0;
    private String businessId;
    private final ProxyClientConfig clientConfig;
    private final String localIP;
    private String localMd5;
    private final ClientMgr clientManager;
    private boolean bShutDown = false;
    private long doworkTime = 0;
    private EncryptConfigEntry userEncryConfigEntry;
    private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();

    private final JsonParser jsonParser = new JsonParser();
    private final Gson gson = new Gson();

    public ProxyConfigManager(final ProxyClientConfig configure, final String localIP, final ClientMgr clientManager) {
        this.clientConfig = configure;
        this.localIP = localIP;
        this.clientManager = clientManager;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public void shutDown() {
        logger.info("Begin to shut down ProxyConfigManager!");
        bShutDown = true;
    }

    @Override
    public void run() {
        while (!bShutDown) {
            try {
                doProxyEntryQueryWork();
                updateEncryptConfigEntry();
                logger.info("ProxyConf update!");
            } catch (Throwable e) {
                logger.error("Refresh proxy ip list runs into exception {}, {}", e.toString(), e.getStackTrace());
                e.printStackTrace();
            }

            /* Sleep some time.240-360s */
            try {
                Random random = new Random();
                int proxyUpdateIntervalSec = this.clientConfig.getProxyUpdateIntervalMinutes() * 60;

                int sleepTimeSec = proxyUpdateIntervalSec;
                if (proxyUpdateIntervalSec > 5) {
                    sleepTimeSec = proxyUpdateIntervalSec + random.nextInt() % (proxyUpdateIntervalSec / 5);
                }
                logger.info("sleep time {}", sleepTimeSec);
                Thread.sleep(sleepTimeSec * 1000);
            } catch (Throwable e2) {
                //
            }
        }
        logger.info("ProxyConfigManager worker existed!");
    }

    /**
     * try to read cache of proxy entry
     *
     * @return
     */
    private ProxyConfigEntry tryToReadCacheProxyEntry(String configCachePath) {
        rw.readLock().lock();
        try {
            File file = new File(configCachePath);
            long diffTime = System.currentTimeMillis() - file.lastModified();

            if (diffTime < clientConfig.getMaxProxyCacheTimeInMs()) {
                JsonReader reader = new JsonReader(new FileReader(configCachePath));
                ProxyConfigEntry proxyConfigEntry = gson.fromJson(reader, ProxyConfigEntry.class);
                logger.info("{} has a backup! {}", businessId, proxyConfigEntry);
                return proxyConfigEntry;
            }
        } catch (Exception ex) {
            logger.warn("try to read local cache, caught {}", ex.getMessage());
        } finally {
            rw.readLock().unlock();
        }
        return null;
    }

    private void tryToWriteCacheProxyEntry(ProxyConfigEntry entry, String configCachePath) {
        rw.writeLock().lock();
        try {
            File file = new File(configCachePath);
            if (!file.getParentFile().exists()) {
                // try to create parent
                file.getParentFile().mkdirs();
            }
            logger.info("try to write {}} to local cache {}", entry, configCachePath);
            FileWriter fileWriter = new FileWriter(configCachePath);
            gson.toJson(entry, fileWriter);
            fileWriter.flush();
            fileWriter.close();
        } catch (Exception ex) {
            logger.warn("try to write local cache, caught {}", ex.getMessage());
        } finally {
            rw.writeLock().unlock();
        }
    }

    private ProxyConfigEntry requestProxyEntryQuietly() {
        try {
            return requestProxyList(this.clientConfig.getProxyIPServiceURL());
        } catch (Exception e) {
            logger.warn("try to request proxy list by http, caught {}", e.getMessage());
        }
        return null;
    }

    /**
     *  get bid config
     *
     * @return proxyConfigEntry
     * @throws Exception
     */
    public ProxyConfigEntry getBidConfigure() throws Exception {
        ProxyConfigEntry proxyEntry;
        String configAddr = clientConfig.getConfStoreBasePath() + businessId;
        if (this.clientConfig.isReadProxyIPFromLocal()) {
            configAddr = configAddr + ".local";
            proxyEntry = getLocalProxyListFromFile(configAddr);
        } else {
            configAddr = configAddr + ".proxyip";

            proxyEntry = tryToReadCacheProxyEntry(configAddr);
            if (proxyEntry == null) {
                proxyEntry = requestProxyEntryQuietly();
                int requestCount = 0;

                while (requestCount < 3 && proxyEntry == null) {
                    proxyEntry = requestProxyEntryQuietly();
                    requestCount += 1;
                    if (proxyEntry == null) {
                        // sleep then retry
                        TimeUnit.MILLISECONDS.sleep(500);
                    }
                }
            }
            if (proxyEntry == null) {
                throw new Exception("Visit manager error, please check log!");
            } else {
                tryToWriteCacheProxyEntry(proxyEntry, configAddr);
            }
        }
        return proxyEntry;
    }

    public void doProxyEntryQueryWork() throws Exception {
        /* Request the configuration from manager. */
        if (localMd5 == null) {
            localMd5 = calcHostInfoMd5(proxyInfoList);
        }
        ProxyConfigEntry proxyEntry = null;
        String configAddr = clientConfig.getConfStoreBasePath() + businessId;
        if (clientConfig.isReadProxyIPFromLocal()) {
            configAddr = configAddr + ".local";
            proxyEntry = getLocalProxyListFromFile(configAddr);
        } else {
            /* Do a compare and see if it needs to re-choose the channel. */
            configAddr = configAddr + ".managerip";
            int retryCount = 1;
            while (proxyEntry == null && retryCount < this.clientConfig.getProxyUpdateMaxRetry()) {
                proxyEntry = requestProxyEntryQuietly();
                retryCount++;
                if (proxyEntry == null) {
                    // sleep then retry.
                    TimeUnit.SECONDS.sleep(1);
                }
            }
            if (proxyEntry != null) {
                tryToWriteCacheProxyEntry(proxyEntry, configAddr);
            }
            /* We should exit if no local IP list and can't request it from manager.*/
            if (localMd5 == null && proxyEntry == null) {
                logger.error("Can't connect manager at the start of proxy API {}",
                        this.clientConfig.getProxyIPServiceURL());
                proxyEntry = tryToReadCacheProxyEntry(configAddr);
            }
            if (localMd5 != null && proxyEntry == null && proxyInfoList != null) {
                StringBuffer s = new StringBuffer();
                for (HostInfo tmp : proxyInfoList) {
                    s.append(tmp.getHostName()).append(";").append(tmp.getPortNumber())
                            .append(",");
                }
                logger.warn("Backup proxyEntry [{}]", s);
            }
        }
        if (localMd5 == null && proxyEntry == null && proxyInfoList == null) {
            if (clientConfig.isReadProxyIPFromLocal()) {
                throw new Exception("Local proxy address configure "
                        + "read failure, please check first!");
            } else {
                throw new Exception("Connect Manager failure, please check first!");
            }
        }
        compareProxyList(proxyEntry);
    }

    /**
     * compare proxy list
     *
     * @param proxyEntry
     */
    private void compareProxyList(ProxyConfigEntry proxyEntry) {
        if (proxyEntry != null) {
            logger.info("{}", proxyEntry.toString());
            if (proxyEntry.getSize() != 0) {
                /* Initialize the current proxy information list first. */
                clientManager.setLoadThreshold(proxyEntry.getLoad());
                clientManager.setBidNum(proxyEntry.getBidNum());
                clientManager.setTidMap(proxyEntry.getTidNumMap());

                List<HostInfo> newProxyInfoList = new ArrayList<HostInfo>();
                for (Map.Entry<String, HostInfo> entry : proxyEntry.getHostMap().entrySet()) {
                    newProxyInfoList.add(entry.getValue());
                }

                String newMd5 = calcHostInfoMd5(newProxyInfoList);
                String oldMd5 = calcHostInfoMd5(proxyInfoList);
                if (newMd5 != null && !newMd5.equals(oldMd5)) {
                    /* Choose random alive connections to send messages. */
                    logger.info("old md5 {} new md5 {}", oldMd5, newMd5);
                    proxyInfoList.clear();
                    proxyInfoList = newProxyInfoList;
                    clientManager.setProxyInfoList(proxyInfoList);
                    doworkTime = System.currentTimeMillis();
                } else if (proxyEntry.getSwitchStat() != oldStat) {
                    /*judge  cluster's switch state*/
                    oldStat = proxyEntry.getSwitchStat();
                    if ((System.currentTimeMillis() - doworkTime) > 3 * 60 * 1000) {
                        logger.info("switch the cluster!");
                        proxyInfoList.clear();
                        proxyInfoList = newProxyInfoList;
                        clientManager.setProxyInfoList(proxyInfoList);
                    } else {
                        logger.info("only change oldStat ");
                    }
                } else {
                    newProxyInfoList.clear();
                    logger.info("proxy IP list doesn't change, load {}", proxyEntry.getLoad());
                }
            } else {
                logger.error("proxyEntry's size is zero");
            }
        }
    }

    public EncryptConfigEntry getEncryptConfigEntry(final String userName) {
        if (Utils.isBlank(userName)) {
            return null;
        }
        EncryptConfigEntry encryptEntry = this.userEncryConfigEntry;
        if (encryptEntry == null) {
            int retryCount = 0;
            encryptEntry = requestPubKey(this.clientConfig.getRsaPubKeyUrl(), userName, false);
            while (encryptEntry == null && retryCount < this.clientConfig.getProxyUpdateMaxRetry()) {
                encryptEntry = requestPubKey(this.clientConfig.getRsaPubKeyUrl(), userName, false);
                retryCount++;
            }
            if (encryptEntry == null) {
                encryptEntry = getStoredPubKeyEntry(userName);
                if (encryptEntry != null) {
                    encryptEntry.getRsaEncryptedKey();
                    synchronized (this) {
                        if (this.userEncryConfigEntry == null) {
                            this.userEncryConfigEntry = encryptEntry;
                        } else {
                            encryptEntry = this.userEncryConfigEntry;
                        }
                    }
                }
            } else {
                synchronized (this) {
                    if (this.userEncryConfigEntry == null || this.userEncryConfigEntry != encryptEntry) {
                        storePubKeyEntry(encryptEntry);
                        encryptEntry.getRsaEncryptedKey();
                        this.userEncryConfigEntry = encryptEntry;
                    } else {
                        encryptEntry = this.userEncryConfigEntry;
                    }
                }
            }
        }
        return encryptEntry;
    }


    private void updateEncryptConfigEntry() {
        if (Utils.isBlank(this.clientConfig.getUserName())) {
            return;
        }
        int retryCount = 0;
        EncryptConfigEntry encryptConfigEntry = requestPubKey(this.clientConfig.getRsaPubKeyUrl(),
                this.clientConfig.getUserName(), false);
        while (encryptConfigEntry == null && retryCount < this.clientConfig.getProxyUpdateMaxRetry()) {
            encryptConfigEntry = requestPubKey(this.clientConfig.getRsaPubKeyUrl(),
                    this.clientConfig.getUserName(), false);
            retryCount++;
        }
        if (encryptConfigEntry == null) {
            return;
        }
        synchronized (this) {
            if (this.userEncryConfigEntry == null || this.userEncryConfigEntry != encryptConfigEntry) {
                storePubKeyEntry(encryptConfigEntry);
                encryptConfigEntry.getRsaEncryptedKey();
                this.userEncryConfigEntry = encryptConfigEntry;
            }
        }
        return;
    }

    private EncryptConfigEntry getStoredPubKeyEntry(String userName) {
        if (Utils.isBlank(userName)) {
            logger.warn(" userName(" + userName + ") is not available");
            return null;
        }
        EncryptConfigEntry entry;
        FileInputStream fis = null;
        ObjectInputStream is = null;
        rw.readLock().lock();
        try {
            File file = new File(clientConfig.getConfStoreBasePath() + userName + ".pubKey");
            if (file.exists()) {
                fis = new FileInputStream(file);
                is = new ObjectInputStream(fis);
                entry = (EncryptConfigEntry) is.readObject();
                //is.close();
                fis.close();
                return entry;
            } else {
                return null;
            }
        } catch (Throwable e1) {
            logger.error("Read " + userName + " stored PubKeyEntry error ", e1);
            return null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Throwable e2) {
                    //
                }
            }
            rw.readLock().unlock();
        }
    }

    private void storePubKeyEntry(EncryptConfigEntry entry) {
        FileOutputStream fos = null;
        ObjectOutputStream p = null;
        rw.writeLock().lock();
        try {
            File file = new File(clientConfig.getConfStoreBasePath() + entry.getUserName() + ".pubKey");
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdir();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            fos = new FileOutputStream(file);
            p = new ObjectOutputStream(fos);
            p.writeObject(entry);
            p.flush();
            //p.close();
        } catch (Throwable e) {
            logger.error("store EncryptConfigEntry " + entry.toString() + " exception ", e);
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Throwable e2) {
                    //
                }
            }
            rw.writeLock().unlock();
        }
    }

    private String calcHostInfoMd5(List<HostInfo> hostInfoList) {
        if (hostInfoList == null || hostInfoList.isEmpty()) {
            return null;
        }
        Collections.sort(hostInfoList);
        StringBuffer hostInfoMd5 = new StringBuffer();
        for (HostInfo hostInfo : hostInfoList) {
            if (hostInfo == null) {
                continue;
            }
            hostInfoMd5.append(hostInfo.getHostName());
            hostInfoMd5.append(";");
            hostInfoMd5.append(hostInfo.getPortNumber());
            hostInfoMd5.append(";");
        }

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            logger.error("{}, {}", e.toString(), e.getStackTrace());
        }
        try {
            md.update(hostInfoMd5.toString().getBytes("utf8"), 0, hostInfoMd5.toString().length());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        byte[] mdbytes = md.digest();
        // convert the byte to hex format method 1
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < mdbytes.length; i++) {
            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    private EncryptConfigEntry requestPubKey(String pubKeyUrl, String userName, boolean needGet) {
        if (Utils.isBlank(userName)) {
            logger.error("Queried userName is null!");
            return null;
        }
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("operation", "query"));
        params.add(new BasicNameValuePair("username", userName));
        JsonObject pubKeyConf = requestConfiguration(pubKeyUrl, params);
        if (pubKeyConf == null) {
            logger.info("No public key information returned from manager");
            return null;
        }
        if (!pubKeyConf.has("resultCode")) {
            logger.info("Parse pubKeyConf failure: No resultCode key information returned from manager");
            return null;
        }
        int resultCode = pubKeyConf.get("resultCode").getAsInt();
        if (resultCode != 0) {
            logger.info("query pubKeyConf failure, error code is " + resultCode + ", errInfo is "
                    + pubKeyConf.get("message").getAsString());
            return null;
        }
        if (!pubKeyConf.has("resultData")) {
            logger.info("Parse pubKeyConf failure: No resultData key information returned from manager");
            return null;
        }
        JsonObject resultData = pubKeyConf.get("resultData").getAsJsonObject();
        if (resultData != null) {
            String publicKey = resultData.get("publicKey").getAsString();
            if (Utils.isBlank(publicKey)) {
                return null;
            }
            String username = resultData.get("username").getAsString();
            if (Utils.isBlank(username)) {
                return null;
            }
            String versionStr = resultData.get("version").getAsString();
            if (Utils.isBlank(versionStr)) {
                return null;
            }
            return new EncryptConfigEntry(username, versionStr, publicKey);
        }
        return null;
    }

    private ProxyConfigEntry getLocalProxyListFromFile(String filePath) throws Exception {
        JsonObject localProxyAddrJson;
        try {
            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
            localProxyAddrJson = jsonParser.parse(new String(fileBytes)).getAsJsonObject();
        } catch (Throwable e) {
            throw new Exception("Read local proxyList File failure by " + filePath + ", reason is " + e.getCause());
        }

        int bidNum = 0;
        if (localProxyAddrJson.has("bsn")) {
            bidNum = localProxyAddrJson.get("bsn").getAsInt();
        }

        int load = ConfigConstants.LOAD_THRESHOLD;
        if (localProxyAddrJson.has("load")) {
            int inLoad = localProxyAddrJson.get("load").getAsInt();
            load = inLoad > 200 ? 200 : (Math.max(inLoad, 0));
        }
        ProxyConfigEntry proxyEntry = new ProxyConfigEntry();
        proxyEntry.setBid(clientConfig.getBid());
        boolean isInterVisit = checkValidProxy(filePath, localProxyAddrJson);
        proxyEntry.setInterVisit(isInterVisit);
        Map<String, HostInfo> hostMap = getHostInfoMap(
            localProxyAddrJson);
        proxyEntry.setHostMap(hostMap);
        proxyEntry.setSwitchStat(0);
        Map<String, Integer> tidMap = getTidMap(localProxyAddrJson);
        proxyEntry.setBidNumAndTidNumMap(bidNum, tidMap);
        proxyEntry.setLoad(load);
        if (localProxyAddrJson.has("cluster_id")) {
            proxyEntry.setClusterId(localProxyAddrJson.get("cluster_id").getAsString());
        }
        return proxyEntry;
    }


    private Map<String, HostInfo> getHostInfoMap(JsonObject localProxyAddrJson)
        throws Exception {
        Map<String, HostInfo> hostMap = new HashMap<String, HostInfo>();
        JsonArray jsonHostList = localProxyAddrJson.get("address").getAsJsonArray();
        if (jsonHostList == null) {
            throw new Exception("Parse local proxyList failure: address field is not exist!");
        }
        for (int i = 0; i < jsonHostList.size(); i++) {
            JsonObject jsonItem = jsonHostList.get(i).getAsJsonObject();
            if (jsonItem != null) {
                if (!jsonItem.has("port")) {
                    throw new Exception("Parse local proxyList failure: "
                        + "port field is not exist in address(" + i + ")!");
                }
                int port = jsonItem.get("port").getAsInt();
                if (port <= 0) {
                    throw new Exception("Parse local proxyList failure: "
                        + "port value <= 0 in address(" + i + ")!");
                }
                if (!jsonItem.has("host")) {
                    throw new Exception("Parse local proxyList failure: "
                        + "host field is not exist in address(" + i + ")!");
                }
                String hostItem = jsonItem.get("host").getAsString();
                if (Utils.isBlank(hostItem)) {
                    throw new Exception("Parse local proxyList failure: "
                        + "host value is blank in address(" + i + ")!");
                }
                String refId = hostItem + ":" + String.valueOf(port);
                hostMap.put(refId, new HostInfo(refId, hostItem, port));
            }
        }
        if (hostMap.isEmpty()) {
            throw new Exception("Parse local proxyList failure: address is empty !");
        }
        return hostMap;
    }

    private Map<String, Integer> getTidMap(JsonObject localProxyAddrJson) {
        Map<String, Integer> tidMap = new HashMap<String, Integer>();
        if (localProxyAddrJson.has("tsn")) {
            JsonArray jsontid = localProxyAddrJson.getAsJsonArray("tsn");
            for (int i = 0; i < jsontid.size(); i++) {
                JsonObject jsonItem = jsontid.get(i).getAsJsonObject();
                if (jsonItem != null && jsonItem.has("tid") && jsonItem.has("sn")) {
                    tidMap.put(jsonItem.get("tid").getAsString(), jsonItem.get("sn").getAsInt());
                }
            }
        }
        return tidMap;
    }

    private boolean checkValidProxy(String filePath, JsonObject localProxyAddrJson) throws Exception {
        if (localProxyAddrJson == null) {
            throw new Exception("Read local proxyList File failure by " + filePath + ", reason is content is null!");
        }
        if (!localProxyAddrJson.has("size")) {
            throw new Exception("Parse local proxyList failure: size field is not exist!");
        }
        int size = localProxyAddrJson.get("size").getAsInt();
        if (size == 0) {
            throw new Exception("Parse local proxyList failure: proxy list size = 0!");
        }
        boolean isInterVisit = false;
        if (localProxyAddrJson.has("isInterVisit")) {
            isInterVisit = localProxyAddrJson.get("isInterVisit").getAsInt() != 0;
        }
        return isInterVisit;
    }


    public ProxyConfigEntry requestProxyList(String url) {
        ArrayList<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("netTag", clientConfig.getNetTag()));
        params.add(new BasicNameValuePair("ip", this.localIP));
        logger.info("Begin to get configure from manager {}, param is {}", url, params);

        JsonObject jsonRes = requestConfiguration(url, params);
        if (jsonRes == null) {
            return null;
        }

        Map<String, HostInfo> hostMap = formatHostInfoMap(
            jsonRes);

        if (hostMap == null) {
            return null;
        }
        int bidNum = 0;
        if (jsonRes.has("bsn")) {
            bidNum = jsonRes.get("bsn").getAsInt();
        }
        int load = ConfigConstants.LOAD_THRESHOLD;
        if (jsonRes.has("load")) {
            int inLoad = jsonRes.get("load").getAsInt();
            load = inLoad > 200 ? 200 : (Math.max(inLoad, 0));
        }
        ProxyConfigEntry proxyEntry = new ProxyConfigEntry();
        proxyEntry.setBid(clientConfig.getBid());
        proxyEntry.setInterVisit(true);
        proxyEntry.setHostMap(hostMap);
        proxyEntry.setSwitchStat(0);
        Map<String, Integer> tidMap = getTidMap(jsonRes);
        proxyEntry.setBidNumAndTidNumMap(bidNum, tidMap);
        proxyEntry.setLoad(load);
        if (jsonRes.has("cluster_id")) {
            proxyEntry.setClusterId(jsonRes.get("cluster_id").getAsString());
        }

        return proxyEntry;
    }

    private Map<String, HostInfo> formatHostInfoMap(JsonObject jsonRes) {
        Map<String, HostInfo> hostMap = new HashMap<String, HostInfo>();
        JsonArray jsonHostList = jsonRes.getAsJsonArray("data");
        if (jsonHostList == null) {
            logger.info("Parse proxyList failure: address field is not exist for response from manager!");
            return null;
        }
        for (int i = 0; i < jsonHostList.size(); i++) {
            JsonObject jsonItem = jsonHostList.get(i).getAsJsonObject();
            if (jsonItem != null) {
                if (!jsonItem.has("port")) {
                    logger.error("Parse proxyList failure: port field is not exist in address("
                            + i + ") for response from manager!");
                    return null;
                }
                int port = jsonItem.get("port").getAsInt();
                if (port <= 0) {
                    logger.info("Parse proxyList failure: port value <= 0 in address("
                            + i + ") for response from manager!");
                    return null;
                }
                if (!jsonItem.has("ip")) {
                    logger.error("Parse proxyList failure: host field is not exist in address("
                            + i + ") for response from manager!");
                    return null;
                }
                String hostItem = jsonItem.get("ip").getAsString();
                if (Utils.isBlank(hostItem)) {
                    logger.error("Parse proxyList failure: host value is blank in address("
                            + i + ") for response from manager!");
                    return null;
                }
                String refId = hostItem + ":" + String.valueOf(port);
                hostMap.put(refId, new HostInfo(refId, hostItem, port));
            }
        }
        if (hostMap.isEmpty()) {
            logger.error("Parse proxyList failure: address is empty for response from manager!");
            return null;
        }
        return hostMap;
    }

    private String updateUrl(String url, int tryIdx, String localManagerIpList) {
        if (tryIdx == 0) {
            return url;
        }

        int headerIdx = url.indexOf("://");
        if (headerIdx == -1) {
            return null;
        }
        String header = "";
        header = url.substring(0, headerIdx + 3);
        String tmpUrl = url.substring(headerIdx + 3);
        int tailerIdx = tmpUrl.indexOf("/");
        if (tailerIdx == -1) {
            return null;
        }
        String tailer = "";
        tailer = tmpUrl.substring(tailerIdx);
        String[] managerIps = localManagerIpList.split(",");
        String currentManagerIp = "";
        int idx = 1;
        for (String managerIp : managerIps) {
            if (idx++ == tryIdx) {
                currentManagerIp = managerIp;
                break;
            }
        }
        if (!currentManagerIp.equals("")) {
            return header + currentManagerIp + ":" + clientConfig.getManagerPort() + tailer;
        }
        return null;
    }

    /* Request new configurations from Manager. */
    private JsonObject requestConfiguration(String url, List<BasicNameValuePair> params) {
        if (Utils.isBlank(url)) {
            logger.error("request url is null");
            return null;
        }
        // get local managerIpList
        String localManagerIps = "";
        int tryIdx = 0;
        while (true) {
            HttpPost httpPost = null;
            String returnStr = null;
            HttpParams myParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(myParams, 10000);
            HttpConnectionParams.setSoTimeout(myParams, clientConfig.getManagerSocketTimeout());
            CloseableHttpClient httpClient = null;
            if (this.clientConfig.isLocalVisit()) {
                httpClient = new DefaultHttpClient(myParams);
            } else {
                try {
                    httpClient = getCloseableHttpClient(params);
                } catch (Throwable eHttps) {
                    logger.error("Create Https cliet failure, error 1 is ", eHttps);
                    eHttps.printStackTrace();
                    return null;
                }
            }

            if (!clientConfig.isEnableSaveManagerVIps() && tryIdx > 0) {
                return null;
            }
            // change url's manager host port when occur error
            url = updateUrl(url, tryIdx, localManagerIps);
            if (url == null) {
                return null;
            }
            tryIdx++;

            logger.info("Request url : " + url + ", localManagerIps : " + localManagerIps);
            try {
                httpPost = new HttpPost(url);
                if (this.clientConfig.isNeedAuthentication()) {
                    long timestamp = System.currentTimeMillis();
                    int nonce = new SecureRandom(String.valueOf(timestamp).getBytes()).nextInt(Integer.MAX_VALUE);
                    httpPost.setHeader(REQUEST_HEADER_AUTHORIZATION,
                            Utils.getAuthorizenInfo(clientConfig.getUserName(),
                                    clientConfig.getSecretKey(), timestamp, nonce));
                }

                StringEntity se = getEntity(params);
                httpPost.setEntity(se);
                HttpResponse response = httpClient.execute(httpPost);
                returnStr = EntityUtils.toString(response.getEntity());
                if (Utils.isNotBlank(returnStr) && response.getStatusLine().getStatusCode() == 200) {
                    logger.info("Get configure from manager is " + returnStr);
                    JsonObject jsonRes = jsonParser.parse(returnStr).getAsJsonObject();
                    return jsonRes;
                }

                if (!clientConfig.isLocalVisit()) {
                    return null;
                }
            } catch (Throwable e) {
                logger.error("Connect Manager error, message: {}, url is {}", e.getMessage(), url);

                if (!clientConfig.isLocalVisit()) {
                    return null;
                }
                // get localManagerIps
                localManagerIps = getLocalManagerIps();
                if (localManagerIps == null) {
                    return null;
                }
            } finally {
                if (httpPost != null) {
                    httpPost.releaseConnection();
                }
                if (httpClient != null) {
                    httpClient.getConnectionManager().shutdown();
                }
            }
        }
    }

    private StringEntity getEntity(List<BasicNameValuePair> params)
        throws UnsupportedEncodingException {
        JsonObject jsonObject = new JsonObject();
        for (BasicNameValuePair pair : params) {
            jsonObject.addProperty(pair.getName(), pair.getValue());
        }
        StringEntity se = new StringEntity(jsonObject.toString());
        se.setContentType(APPLICATION_JSON);
        return se;
    }

    private CloseableHttpClient getCloseableHttpClient(List<BasicNameValuePair> params)
        throws NoSuchAlgorithmException, KeyManagementException {
        CloseableHttpClient httpClient;
        ArrayList<Header> headers = new ArrayList<Header>();
        for (BasicNameValuePair paramItem : params) {
            headers.add(new BasicHeader(paramItem.getName(), paramItem.getValue()));
        }
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(10000)
                .setSocketTimeout(clientConfig.getManagerSocketTimeout()).build();
        SSLContext sslContext = SSLContexts.custom().build();
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext,
                new String[]{"TLSv1"}, null,
                SSLConnectionSocketFactory.getDefaultHostnameVerifier());
        httpClient = HttpClients.custom().setDefaultHeaders(headers).setDefaultRequestConfig(requestConfig)
                .setSSLSocketFactory(sslsf).build();
        return httpClient;
    }

    private String getLocalManagerIps() {
        String localManagerIps;
        try {
            File localManagerIpsFile = new File(clientConfig.getManagerIpLocalPath());
            if (localManagerIpsFile.exists()) {
                byte[] serialized;
                serialized = FileUtils.readFileToByteArray(localManagerIpsFile);
                if (serialized == null) {
                    logger.error("Local managerIp file is empty, file path : "
                                + clientConfig.getManagerIpLocalPath());
                    return null;
                }
                localManagerIps = new String(serialized, "UTF-8");
            } else {
                if (!localManagerIpsFile.getParentFile().exists()) {
                    localManagerIpsFile.getParentFile().mkdirs();
                }
                localManagerIps = "";
                logger.error("Get local managerIpList not exist, file path : "
                            + clientConfig.getManagerIpLocalPath());
            }
        } catch (Throwable t) {
            localManagerIps = "";
            logger.error("Get local managerIpList occur exception,", t);
        }
        return localManagerIps;
    }
}
