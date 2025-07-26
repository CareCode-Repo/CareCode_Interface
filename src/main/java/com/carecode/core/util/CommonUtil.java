package com.carecode.core.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class CommonUtil {

    /**
     * Hash Encrypt Method
     * return 46 Byte String word
     */
    public static String getHashEncryptSha256(String planText) throws Exception {

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(planText.getBytes());
        byte byteData[] = md.digest();

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }

        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
            String hex = Integer.toHexString(0xff & byteData[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();

    }


    /**
     * HttpServletRequest to HashMap
     *
     * @param request
     * @return
     * @throws Exception
     */
    @SuppressWarnings("static-access")
    public static HashMap<String, Object> getParameterMap(HttpServletRequest request) throws IOException {

        HashMap<String, Object> paramMap = new HashMap<String, Object>();

        Enumeration<String> e = request.getParameterNames();

        while (e.hasMoreElements()) {
            String key = e.nextElement();
            String[] values = request.getParameterValues(key);
            if (values.length == 1) {
                paramMap.put(key, values[0].trim());

                if (!CommonUtil.isScheduleRunning())
                    System.out.println("key=[" + key + "][" + values[0].trim() + "]");
            } else {
                paramMap.put(key, values);

                if (!CommonUtil.isScheduleRunning()) System.out.println("key=[" + key + "][" + values + "]");
            }
        }

        /** Push Map to Request Attribute **/
        Enumeration<String> attributeNames = (Enumeration<String>) request.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String key = (String) attributeNames.nextElement();
            Object obj = request.getAttribute(key);
            if (obj instanceof java.lang.String) {

                if (!CommonUtil.isScheduleRunning()) System.out.println("key=[" + key + "][" + obj + "]");

                paramMap.put(key, obj);
            }
        }

        /** Push Map to MultipartHttpServletRequest **/
        MultipartHttpServletRequest mpRequest = null;
        if (request instanceof MultipartHttpServletRequest) {
            mpRequest = (MultipartHttpServletRequest) request;
            Iterator<String> iter = mpRequest.getFileNames();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                MultipartFile file = mpRequest.getFile(key);
                if (file.getSize() > 0) {
                    paramMap.put(key, file);
                }
            }
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = (Map<String, Object>) (request.getSession().getAttribute("authUserInfo"));

        Locale localInfo = (Locale) (request.getSession().getAttribute("LocalInfo"));
        paramMap.put("LOGIN_LOCALE", localInfo.getDefault().toString().toUpperCase());

        if (!CommonUtil.isScheduleRunning())
            System.out.println("############# LOGIN_LOCALE=[" + localInfo.getDefault().toString().toUpperCase() + "]");


        //device_code : ACCESS IP, VENDOR, ID, NAME, DEPT_CODE
        InetAddress local = InetAddress.getLocalHost();

        if (userInfo != null && userInfo.containsKey("USER_ID")) {
            paramMap.put("LOGIN_USER_ID", userInfo.get("USER_ID"));
            paramMap.put("LOGIN_USER_NAME", userInfo.get("USER_NM"));
        }

        paramMap.put("LOGIN_USER_SESSION", request.getSession().getId());

        //System.out.println("paramMap=[" + paramMap.toString() + "]");

        return paramMap;
    }

    /**
     * 공통 코드를 Session에 넣기 위해서 HashMap에 Group Code를  String 이름으로 저장함
     *
     * @param list
     * @return
     * @throws Exception
     */
    public static HashMap<String, Object> getCode(List<Map<String, Object>> list) throws Exception {
        HashMap<String, Object> resultMap = new HashMap<String, Object>();
        String preGrpName = null;
        int i = 1;
        Iterator<Map<String, Object>> it = list.iterator();
        List<HashMap<String, Object>> tempList = new ArrayList<HashMap<String, Object>>();
        while (it.hasNext()) {
            HashMap<String, Object> map = (HashMap<String, Object>) it.next();
            String hCode = "H_" + (String) map.get("HIGH_CD");
            if ((i != 1 && !hCode.equals(preGrpName))) {
                resultMap.put(preGrpName, tempList);
                tempList = new ArrayList<HashMap<String, Object>>();
                tempList.add(map);
                if (i == list.size()) {
                    resultMap.put(hCode, tempList);
                    tempList = new ArrayList<HashMap<String, Object>>();
                }
            } else {
                tempList.add(map);
                if (i == list.size()) {
                    if (preGrpName == null) {
                        preGrpName = hCode;
                    }
                    resultMap.put(preGrpName, tempList);
                    tempList = new ArrayList<HashMap<String, Object>>();
                }
            }
            preGrpName = hCode;
            i++;
        }
        return resultMap;
    }

    /**
     * 공통 코드를 Session에 넣기 위해서 HashMap에 Group Code를  String 이름으로 저장함
     *
     * @param list
     * @return
     * @throws Exception
     */
    public static HashMap<String, Object> getDomainLocaleMap(List<Map<String, Object>> list) throws Exception {
        HashMap<String, Object> resultMap = new HashMap<String, Object>();
        Iterator<Map<String, Object>> it = list.iterator();
        while (it.hasNext()) {
            HashMap<String, Object> map = (HashMap<String, Object>) it.next();
            resultMap.put((String) map.get("DOMAIN_CD"), (String) map.get("DOMAIN_NM"));
        }
        return resultMap;
    }

    /**
     * Random 번호 추출
     *
     * @return
     */
    public static String getUUIDString() {
        return UUID.randomUUID().toString();
    }

    /**
     * Random 번호 추출
     *
     * @return
     */
    public static String getUUIDString(String sType) {
        UUID uuid = UUID.randomUUID();
        return "key".equals(sType) ? uuid.toString().replaceAll("-", "").substring(0, 18) : uuid.toString().replaceAll("-", "");
    }


    /**
     * getHashString :MD5 해시 암호화
     *
     * @param sOrgValue
     * @return String
     */
    public static String getHashString(String sOrgValue) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(sOrgValue.getBytes());
            return toHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return sOrgValue;
        }
    }

    private static String toHex(byte hash[]) {
        StringBuffer buf = new StringBuffer(hash.length * 2);
        for (int i = 0; i < hash.length; i++) {
            int intVal = hash[i] & 0xff;
            if (intVal < 0x10) {
                buf.append("0");
            }
            buf.append(Integer.toHexString(intVal));
        }
        return buf.toString();
    }

    public static String NVL(Object sData, String rData) throws Exception {

        if (sData == null || "".equals(sData)) {
            return rData;
        }

        return sData.toString().trim();

    }

    public static Double NVL2(String sData, Double rData) throws Exception {

        if (sData == null || "".equals(sData)) {
            return rData;
        }

        return Double.parseDouble(sData);

    }

    /**
     * 주어진 숫자의 왼쪽에 모자란 길이만큼 문자 '0'으로 채워서 돌려 준다.
     *
     * @param integer    Pad를 만들어 붙여주어야 할 int 값
     * @param fullLength Padding 했을 때 전체 문자열 폭
     * @author ShinWon Kang
     * @date 2002-06-17
     */
    public static String pad(int integer, int fullLength) {
        return padLeftWithZero(integer + "", fullLength);
    }


    /**
     * 주어진 숫자의 왼쪽에 모자란 길이만큼 문자 '0'으로 채워서 돌려 준다.
     *
     * @param integer    Pad를 만들어 붙여주어야 할 int 값
     * @param fullLength Padding 했을 때 전체 문자열 폭
     * @author ShinWon Kang
     * @date 2002-06-28
     */
    public static String padLeftWithZero(int integer, int fullLength) {
        return padLeftWithZero(integer + "", fullLength);
    }

    /**
     * 주어진 문자열의 왼쪽에 모자란 길이만큼 문자 '0'으로 채워서 돌려 준다.
     *
     * @param str        Pad를 만들어 붙여주어야 할 문자열
     * @param fullLength Padding 했을 때 전체 문자열 폭
     * @author ShinWon Kang
     * @date 2002-06-17
     */
    public static String padLeftWithZero(String str, int fullLength) {
        return padLeft(str, '0', fullLength);
    }


    /**
     * 주어진 문자열의 오른쪽에 모자란 길이만큼 문자 '0'으로 채워서 돌려 준다.
     *
     * @param str        Pad를 만들어 붙여주어야 할 문자열
     * @param fullLength Padding 했을 때 전체 문자열 폭
     * @author ShinWon Kang
     * @date 2002-06-17
     */
    public static String padRightWithZero(String str, int fullLength) {
        return padRight(str, '0', fullLength);
    }

    public final static int PADDING_LEFT = 0;
    public final static int PADDING_RIGHT = 1;

    /**
     * 주어진 문자열의 왼쪽에 모자란 길이만큼 주어진 문자로 채워서 돌려 준다.
     *
     * @param str        Pad를 만들어 붙여주어야 할 문자열
     * @param padChar    Padding에 사용될 문자
     * @param fullLength Padding 했을 때 전체 문자열 폭
     * @author ShinWon Kang
     * @date 2002-06-17
     */
    public static String padLeft(String str, char padChar, int fullLength) {
        return pad(str, padChar, fullLength, PADDING_LEFT);
    }


    /**
     * 주어진 문자열의 오른쪽에 모자란 길이만큼 주어진 문자로 채워서 돌려 준다.
     *
     * @param str        Pad를 만들어 붙여주어야 할 문자열
     * @param padChar    Padding에 사용될 문자
     * @param fullLength Padding 했을 때 전체 문자열 폭
     * @author ShinWon Kang
     * @date 2002-06-17
     */
    public static String padRight(String str, char padChar, int fullLength) {
        return pad(str, padChar, fullLength, PADDING_RIGHT);
    }


    /**
     * 주어진 문자열의 왼쪽이나나 오른쪽에 모자란 길이만큼 주어진 문자로
     * 채워서 돌려 준다.
     *
     * @param str        Pad를 만들어 붙여주어야 할 문자열
     * @param padChar    Padding에 사용될 문자
     * @param fullLength Padding 했을 때 전체 문자열 폭
     * @author ShinWon Kang
     * @date 2002-06-17
     */
    public static String pad(String str, char padChar, int fullLength, int paddingDir) {
        if (str == null) return null;

        int size = str.getBytes().length;

        if (size >= fullLength) return str;

        int padCount = fullLength - size;
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < padCount; i++) {
            buff.append(padChar);
        }

        if (paddingDir == PADDING_LEFT) {
            str = buff.append(str).toString();
        }
        if (paddingDir == PADDING_RIGHT) {
            str = str + buff;
        }

        return str;
    }

    /*********************************************************
     * null인경우 blank string을 return
     *********************************************************/
    public static String nullToBlank(String str) {
        if (str == null || str.equals("")) str = "";
        return str.trim();
    }

    /*********************************************************
     * null인경우 space string을 return
     *********************************************************/
    public static String nullToSpace(String str) {
        if (str == null || str.equals("")) str = " ";
        return str.trim();
    }

    /*********************************************************
     * null인경우 zero string을 return
     *********************************************************/
    public static String nullToZero(String str) {
        if (str == null || str.equals("")) str = "0";
        return str.trim();
    }

    /*********************************************************
     * 현재날짜를 14자리 숫자로 반환 (YKK-Vietnam date type : numeric(14))
     *********************************************************/
    public static Long getTodayReturnAsNumber() {
        String regdt = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
        return Long.parseLong(regdt);
    }

    public static boolean isScheduleRunning() {
        boolean isRunning = false;
        try {
            InetAddress local = InetAddress.getLocalHost();
            String ip = local.getHostAddress();
            System.out.println("local.getHostAddress=[" + ip + "]");
            if ("127.0.1.1".equals(ip)) {
                isRunning = true;
            }
        } catch (Exception exception) {
        }
        return isRunning;
    }

    /**
     * 브라우저 종류
     *
     * @param request
     * @return
     */
    public static String getBrowser(HttpServletRequest request) {
        String header = request.getHeader("User-Agent");
        if (header.indexOf("MSIE") > -1) {
            return "MSIE";
        } else if (header.indexOf("Chrome") > -1) {
            return "Chrome";
        } else if (header.indexOf("Opera") > -1) {
            return "Opera";
        }
        return "Firefox";
    }

    public static String getDisposition(String filename, String browser) throws UnsupportedEncodingException {
        String dispositionPrefix = "attachment;filename=";
        String encodedFilename = null;
        if (browser.equals("MSIE")) {
            encodedFilename = URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20");
        } else if (browser.equals("Firefox")) {
            encodedFilename = "" + new String(filename.getBytes("UTF-8"), "8859_1") + "";
        } else if (browser.equals("Opera")) {
            encodedFilename = "" + new String(filename.getBytes("UTF-8"), "8859_1") + "";
        } else if (browser.equals("Chrome")) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < filename.length(); i++) {
                char c = filename.charAt(i);
                if (c > '~') {
                    sb.append(URLEncoder.encode("" + c, "UTF-8"));
                } else {
                    sb.append(c);
                }
            }
            encodedFilename = sb.toString();
        } else {
            throw new RuntimeException("Not supported browser");
        }
        return dispositionPrefix + encodedFilename;
    }

    public static String makeParameterString(HttpServletRequest request) throws IOException {

        StringBuffer sb = new StringBuffer("");
        int i = 0;
        Enumeration<String> e = request.getParameterNames();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            String[] values = request.getParameterValues(key);
            if (i > 0) sb.append(", ");
            if (values.length == 1) {
                sb.append(key + "=[" + values[0].trim() + "]");
            } else {
                sb.append(key + "=[" + values + "]");
            }
        }

        /** Push Map to Request Attribute **/
        Enumeration<String> attributeNames = (Enumeration<String>) request.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String key = (String) attributeNames.nextElement();
            Object obj = request.getAttribute(key);
            if (obj instanceof java.lang.String) {
                sb.append(key + "=[" + obj.toString() + "]");
            }
        }

        return sb.toString();
    }

    public static File getMultiPartFileToFile(MultipartFile multipartFile) throws IOException {
        File file = new File(multipartFile.getOriginalFilename());
        multipartFile.transferTo(file);
        return file;
    }

    public static void createFileDirectory(File filePath) throws Exception {
        if (!filePath.exists()) {
            if (!filePath.mkdirs()) {
                throw new Exception(
                        "Fail to create a directory for attached file [" + filePath + "]");
            }
        }
    }

    /**
     * Folder Copy
     *
     * @param sourceDir
     * @param targetDir
     * @throws IOException
     */
    public static void copyTransferFolder(File sourceDir, File targetDir) throws IOException {
        if (sourceDir.isDirectory()) {
            String[] files = sourceDir.list();
            for (int i = 0; i < files.length; i++) {
                String file = files[i];
                copyTransferFolder(new File(sourceDir, file), new File(targetDir, file));
            }
        } else {
            copyTransferFile(sourceDir, targetDir);
        }
    }

    /**
     * File Copy
     *
     * @param sourceFile
     * @param targetFile
     * @throws IOException
     */
    public static void copyTransferFile(File sourceFile, File targetFile) throws IOException {

        String sTargetPath = targetFile.getParent();
        if (!new File(sTargetPath).exists()) {
            targetFile.mkdirs();
        }
        Files.copy(sourceFile.toPath(), targetFile.toPath(), REPLACE_EXISTING);
    }

    /**
     * Folder in File Delete
     *
     * @param sourceDir
     * @throws IOException
     */
    public static void deleteFolder(String sourceDir) throws IOException {
        deleteFolder(new File(sourceDir));
    }

    /**
     * Folder in File Delete
     *
     * @param sourceDir
     */
    public static void deleteFolder(File sourceDir) throws IOException {
        if (sourceDir.isDirectory()) {
            String[] files = sourceDir.list();
            for (int i = 0; i < files.length; i++) {
                String file = files[i];
                deleteFolder(new File(sourceDir, file));
            }
            sourceDir.delete();
        } else {
            deleteFile(sourceDir);
        }
    }

    /**
     * File Delete
     *
     * @param sourceFile
     */
    public static void deleteFile(String sourceFile) throws IOException {
        deleteFile(new File(sourceFile));
    }

    /**
     * Folder in File Delete
     *
     * @param sourceDir
     */
    public static void deleteFile(File sourceDir) throws IOException {
        if (sourceDir.exists()) {
            Files.delete(Paths.get(sourceDir.getAbsolutePath()));
        }
    }

    public static String getServerType() {
        String OS = System.getProperty("os.name").toLowerCase();
        if (OS.contains("win")) {
            return "window";
        } else {
            return "linux";
        }
    }

    public static String getCurrentAlarmDate(String dateFormat) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
        return simpleDateFormat.format(new Date());
    }
}
