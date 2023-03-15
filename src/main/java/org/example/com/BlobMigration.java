/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.example.com;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlobMigration {

    public static final String PEM_BEGIN_CERTFICATE = "-----BEGIN CERTIFICATE-----";
    public static final String PEM_END_CERTIFICATE = "-----END CERTIFICATE-----";

    public static void main(String[] args) {

        ReadConfigFile configs = new ReadConfigFile();
        String oracleUrl = configs.getProperty("CONNECTION.ORACLE.URL");
        String oracleDriverClass = configs.getProperty("CONNECTION.ORACLE.DRIVERCLASS");
        String oracleDriverLocation = configs.getProperty("CONNECTION.ORACLE.JDBCDRIVER");
        String oracleIdentityDBUsername = configs.getProperty("CONNECTION.ORACLE.IDENTITYDB.USERNAME");
        String oracleIdentityDBPassword = configs.getProperty("CONNECTION.ORACLE.IDENTITYDB.PASSWORD");
        String oracleRegDBUsername = configs.getProperty("CONNECTION.ORACLE.REGDB.USERNAME");
        String oracleRegDBPassword = configs.getProperty("CONNECTION.ORACLE.REGDB.PASSWORD");
        //String oracleUserstoreDBUsername = configs.getProperty("CONNECTION.ORACLE.USERSTOREDB.USERNAME");
        //String oracleUserstoreDBPassword = configs.getProperty("CONNECTION.ORACLE.USERSTOREDB.PASSWORD");

        String mysqlDriverClass = configs.getProperty("CONNECTION.MYSQL.DRIVERCLASS");
        String mysqlDriverLocation = configs.getProperty("CONNECTION.MYSQL.JDBCDRIVER");
        String mysqlIdentityDBUrl = configs.getProperty("CONNECTION.MYSQL.IDENTITYDB.URL");
        String mysqlIdentityDBUsername = configs.getProperty("CONNECTION.MYSQL.IDENTITYDB.USERNAME");
        String mysqlIdentityDBPassword = configs.getProperty("CONNECTION.MYSQL.IDENTITYDB.PASSWORD");
        String mysqlRegDBUrl = configs.getProperty("CONNECTION.MYSQL.REGDB.URL");
        String mysqlRegDBUsername = configs.getProperty("CONNECTION.MYSQL.REGDB.USERNAME");
        String mysqlRegDBPassword = configs.getProperty("CONNECTION.MYSQL.REGDB.PASSWORD");
        //String mysqlUserstoreDBUrl = configs.getProperty("CONNECTION.MYSQL.USERSTOREDB.URL");
        //String mysqlUserstoreDBUsername = configs.getProperty("CONNECTION.MYSQL.USERSTOREDB.USERNAME");
        //String mysqlUserstoreDBPassword = configs.getProperty("CONNECTION.MYSQL.USERSTOREDB.PASSWORD");

        String operation = configs.getProperty("CLIENT.OPERATION");
        String exportDirectoryPath = configs.getProperty("EXPORT.DIRECTORY.PATH");
        String importDirectoryPath = configs.getProperty("IMPORT.DIRECTORY.PATH");

        if (operation.equals("EXPORT")) {
            File directory = new File(exportDirectoryPath);
            boolean directoryCreated = directory.mkdir();
            if (directoryCreated) {
                DBConnection.loadDBDriver(oracleDriverLocation, oracleDriverClass);
                //exportUMTenant(oracleUrl, oracleUserstoreDBUsername, oracleUserstoreDBPassword, exportDirectoryPath);
                //System.out.println("====== Tenant Data Exported Successfully ======");
                exportRegContent(oracleUrl, oracleRegDBUsername, oracleRegDBPassword, exportDirectoryPath);
                System.out.println("====== Reg Content Data Exported Successfully ======");
                exportAdaptiveScript(oracleUrl, oracleIdentityDBUsername, oracleIdentityDBPassword, exportDirectoryPath);
                System.out.println("====== Adaptive Auth Scripts Exported Successfully ======");
            } else {
                System.out.println("Error: Failed to create new directory.");
            }
        } else if (operation.equals("IMPORT")) {
            DBConnection.loadDBDriver(mysqlDriverLocation, mysqlDriverClass);
            //importUMTenant(mysqlUserstoreDBUrl, mysqlUserstoreDBUsername, mysqlUserstoreDBPassword, importDirectoryPath);
            //System.out.println("====== Tenant Data Imported Successfully ======");
            importRegContent(mysqlRegDBUrl, mysqlRegDBUsername, mysqlRegDBPassword, importDirectoryPath);
            System.out.println("====== Reg Content Data Imported Successfully ======");
            importAdaptiveScript(mysqlIdentityDBUrl, mysqlIdentityDBUsername, mysqlIdentityDBPassword, importDirectoryPath);
            System.out.println("====== Adaptive Auth Scripts Imported Successfully ======");
        }
    }

    public static void exportUMTenant(String oracleUrl, String oracleUsername, String oraclePassword, String directoryPath) {

        List<String[]> rows = new ArrayList<>();

        try {
            Connection oracleConn = DBConnection.getOracleConnection(oracleUrl, oracleUsername, oraclePassword);
            String selectQuery = "SELECT UM_ID, UM_USER_CONFIG FROM UM_TENANT";
            Statement stmt = oracleConn.createStatement();
            ResultSet rs = stmt.executeQuery(selectQuery);

            while (rs.next()) {
                int id = rs.getInt("UM_ID");
                InputStream fileConfig = rs.getBinaryStream("UM_USER_CONFIG");
                byte[] fileData = readInputStream(fileConfig);
                rows.add(new String[]{String.valueOf(id), new String(fileData)});
            }

            File csvFile = new File(directoryPath + "/um_tenant.csv");
            try (FileWriter writer = new FileWriter(csvFile)) {
                for (String[] row : rows) {
                    String line = String.join(",,,", row) + "\n";
                    writer.write(line);
                }
            }
            oracleConn.close();
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void importUMTenant(String mysqlUrl, String mysqlUsername, String mysqlPassword, String directoryPath) {

        try {
            Connection mysqlConn = DBConnection.getMysqlConnection(mysqlUrl, mysqlUsername, mysqlPassword);
            String csvFile = directoryPath + "/um_tenant.csv";

            Map<Integer, String> dataMap = new HashMap<>();
            BufferedReader br = Files.newBufferedReader(Paths.get(csvFile));

            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",,,");
                int id = Integer.parseInt(values[0]);
                String text = values[1];
                dataMap.put(id, text);
            }

            String updateQuery = "UPDATE UM_TENANT SET UM_USER_CONFIG = ? WHERE UM_ID = ?";
            PreparedStatement pstmt = mysqlConn.prepareStatement(updateQuery);

            for (Map.Entry<Integer, String> entry : dataMap.entrySet()) {
                int id = entry.getKey();
                String text = entry.getValue();
                byte[] blob = text.getBytes();
                InputStream inputStream = new ByteArrayInputStream(blob);
                pstmt.setBinaryStream(1, inputStream, blob.length);
                pstmt.setInt(2, id);
                pstmt.executeUpdate();
            }
            mysqlConn.close();
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static byte[] readInputStream(InputStream is) throws IOException {
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int length = 0;
        List<Byte> bytes = new ArrayList<>();
        while ((length = is.read(buffer)) != -1) {
            for (int i = 0; i < length; i++) {
                bytes.add(buffer[i]);
            }
        }
        byte[] output = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            output[i] = bytes.get(i);
        }
        return output;
    }

    public static void exportRegContent(String oracleUrl, String oracleUsername, String oraclePassword, String directoryPath) {

        File subDirectory = new File(directoryPath + "/reg_content_blobs");
        boolean directoryCreated = subDirectory.mkdir();

        List<String[]> rows = new ArrayList<>();
        try {
            Connection oracleConn = DBConnection.getOracleConnection(oracleUrl, oracleUsername, oraclePassword);
            String selectQuery = "SELECT REG_CONTENT_ID, REG_TENANT_ID, REG_CONTENT_DATA FROM REG_CONTENT";
            Statement stmt = oracleConn.createStatement();
            ResultSet rs = stmt.executeQuery(selectQuery);

            while (rs.next()) {
                int regContentId = rs.getInt("REG_CONTENT_ID");
                int regTenantId = rs.getInt("REG_TENANT_ID");
                InputStream inputStream = rs.getBinaryStream("REG_CONTENT_DATA");

                String fileName = subDirectory + "/data_" + regContentId + "_" + regTenantId + ".dat";
                File file = new File(fileName);
                FileOutputStream fileOut = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, length);
                }
                rows.add(new String[]{String.valueOf(regContentId), String.valueOf(regTenantId), fileName});

                fileOut.close();
                inputStream.close();
            }

            File csvFile = new File(directoryPath + "/reg_content.csv");
            try (FileWriter writer = new FileWriter(csvFile)) {
                for (String[] row : rows) {
                    // commenting this as it's not supported in java 8
                    //String line = String.join(",,,", row) + "\n";
                    //writer.write(line);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < row.length; i++) {
                        sb.append(row[i]);
                        if (i != row.length - 1) {
                            sb.append(",,,");
                        }
                    }
                    sb.append("\n");
                    writer.write(sb.toString());
                }
            }
            oracleConn.close();
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void importRegContent(String mysqlUrl, String mysqlUsername, String mysqlPassword, String directoryPath) {
        try {
            Connection mysqlConn = DBConnection.getMysqlConnection(mysqlUrl, mysqlUsername, mysqlPassword);
            String csvFile = directoryPath + "/reg_content.csv";
            BufferedReader br = Files.newBufferedReader(Paths.get(csvFile));

            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",,,");
                int regContentId = Integer.parseInt(values[0]);
                int regTenantId = Integer.parseInt(values[1]);
                String filePath = values[2];
                byte[] content = readFile(filePath);

                String updateQuery = "UPDATE REG_CONTENT SET REG_CONTENT_DATA = ? WHERE REG_CONTENT_ID = ? AND REG_TENANT_ID = ?";
                PreparedStatement pstmt = mysqlConn.prepareStatement(updateQuery);
                pstmt.setBytes(1, content);
                pstmt.setInt(2, regContentId);
                pstmt.setInt(3, regTenantId);
                pstmt.executeUpdate();

                pstmt.close();
            }
            mysqlConn.close();
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static byte[] readFile(String filename) throws IOException {
        try (InputStream input = new FileInputStream(filename)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int n;
            while ((n = input.read(buffer)) != -1) {
                output.write(buffer, 0, n);
            }
            return output.toByteArray();
        }
    }

    public static void exportAdaptiveScript(String oracleUrl, String oracleUsername, String oraclePassword, String directoryPath) {

        File subDirectory = new File(directoryPath + "/adaptive_script_blobs");
        boolean directoryCreated = subDirectory.mkdir();
        List<String[]> rows = new ArrayList<>();
        try {
            Connection oracleConn = DBConnection.getOracleConnection(oracleUrl, oracleUsername, oraclePassword);
            String selectQuery = "SELECT TENANT_ID, APP_ID, CONTENT FROM SP_AUTH_SCRIPT";
            Statement stmt = oracleConn.createStatement();
            ResultSet rs = stmt.executeQuery(selectQuery);

            while (rs.next()) {
                int tenantId = rs.getInt("TENANT_ID");
                int appId = rs.getInt("APP_ID");
                InputStream inputStream = rs.getBinaryStream("CONTENT");
                String fileName = subDirectory + "/data_" + appId + "_" + tenantId + ".dat";
                File file = new File(fileName);
                FileOutputStream fileOut = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, length);
                }
                rows.add(new String[]{String.valueOf(tenantId), String.valueOf(appId), fileName});

                fileOut.close();
                inputStream.close();
            }
            File csvFile = new File(directoryPath + "/adaptive_script.csv");
            try (FileWriter writer = new FileWriter(csvFile)) {
                for (String[] row : rows) {
                    //String line = String.join(",,,", row) + "\n";
                    //writer.write(line);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < row.length; i++) {
                        sb.append(row[i]);
                        if (i != row.length - 1) {
                            sb.append(",,,");
                        }
                    }
                    sb.append("\n");
                    writer.write(sb.toString());
                }
            }
            oracleConn.close();
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void importAdaptiveScript(String mysqlUrl, String mysqlUsername, String mysqlPassword, String directoryPath) {

        try {
            Connection mysqlConn = DBConnection.getMysqlConnection(mysqlUrl, mysqlUsername, mysqlPassword);
            String csvFile = directoryPath + "/adaptive_script.csv";
            BufferedReader br = Files.newBufferedReader(Paths.get(csvFile));

            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",,,");
                int tenantId = Integer.parseInt(values[0]);
                int appId = Integer.parseInt(values[1]);
                String filePath = values[2];
                byte[] content = readFile(filePath);

                String updateQuery = "UPDATE SP_AUTH_SCRIPT SET CONTENT = ? WHERE TENANT_ID = ? AND APP_ID = ?";
                PreparedStatement pstmt = mysqlConn.prepareStatement(updateQuery);
                pstmt.setBytes(1, content);
                pstmt.setInt(2, tenantId);
                pstmt.setInt(3, appId);
                pstmt.executeUpdate();

                pstmt.close();
            }
            mysqlConn.close();
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /*public static void migrateIdpCertificate(String oracleUrl, String oracleUsername, String oraclePassword, String mysqlUrl, String mysqlUsername, String mysqlPassword) {
        try {
            Connection oracleConn = DBConnection.getOracleConnection(oracleUrl, oracleUsername, oraclePassword);
            Connection mysqlConn = DBConnection.getMysqlConnection(mysqlUrl, mysqlUsername, mysqlPassword);

            String selectQuery = "SELECT TENANT_ID, NAME, CERTIFICATE FROM IDP";
            Statement stmt = oracleConn.createStatement();
            ResultSet rs = stmt.executeQuery(selectQuery);

            String updateQuery = "UPDATE IDP SET CERTIFICATE = ? WHERE TENANT_ID = ? AND NAME = ?";
            PreparedStatement pstmt = mysqlConn.prepareStatement(updateQuery);

            while (rs.next()) {
                int tenantId = rs.getInt("TENANT_ID");
                String name = rs.getString("NAME");
                InputStream inputStream = rs.getBinaryStream("CERTIFICATE");
                String certificate = getBlobValue(inputStream);
                JSONArray certificateInfoJsonArray = new JSONArray(getCertificateInfoArray(certificate));
                InputStream is = new ByteArrayInputStream(certificateInfoJsonArray.toString().getBytes());
                pstmt.setBinaryStream(1, is, is.available());
                pstmt.setInt(2, tenantId);
                pstmt.setString(3, name);
                pstmt.executeUpdate();
            }
            oracleConn.close();
            mysqlConn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    /*private static String getBlobValue(InputStream is) {

        if (is != null) {
            BufferedReader br = null;
            StringBuilder sb = new StringBuilder();
            String line;
            try {
                br = new BufferedReader(new InputStreamReader(is));
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            } catch (IOException e) {

            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {

                    }
                }
            }
            return sb.toString();
        }
        return null;
    }*/

    /*private static CertificateInfo[] getCertificateInfoArray(String certificateValue) {
        try {
            if (StringUtils.isNotBlank(certificateValue) && !certificateValue.equals("[]")) {
                certificateValue = certificateValue.trim();
                try {
                    return handleJsonFormatCertificate(certificateValue);
                } catch (JSONException e) {
                    // Handle plain text certificate for file based configuration.
                    if (certificateValue.startsWith(PEM_BEGIN_CERTFICATE)) {
                        return handlePlainTextCertificate(certificateValue);
                    } else {
                        // Handle encoded certificate values. While uploading through UI and file based configuration
                        // without begin and end statement.
                        return handleEncodedCertificate(certificateValue);
                    }
                }
            }
            return new CertificateInfo[0];
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error while generating thumbPrint. Unsupported hash algorithm. ");
            e.printStackTrace();
            return new CertificateInfo[0];
        }
    }*/

    /*private static CertificateInfo[] handleJsonFormatCertificate(String certificateValue) throws NoSuchAlgorithmException {

        JSONArray jsonCertificateInfoArray = new JSONArray(certificateValue);
        int lengthOfJsonArray = jsonCertificateInfoArray.length();

        List<CertificateInfo> certificateInfos = new ArrayList<>();
        for (int i = 0; i < lengthOfJsonArray; i++) {
            JSONObject jsonCertificateInfoObject = (JSONObject) jsonCertificateInfoArray.get(i);
            String thumbPrint = jsonCertificateInfoObject.getString("thumbPrint");

            CertificateInfo certificateInfo = new CertificateInfo();
            certificateInfo.setThumbPrint(thumbPrint);
            certificateInfo.setCertValue(jsonCertificateInfoObject.getString("certValue"));
            certificateInfos.add(certificateInfo);
        }
        return certificateInfos.toArray(new CertificateInfo[lengthOfJsonArray]);
    }*/

    /*private static CertificateInfo[] handlePlainTextCertificate(String certificateValue) throws NoSuchAlgorithmException {

        return createEncodedCertificateInfo(certificateValue, false);
    }*/

    /*private static CertificateInfo[] createEncodedCertificateInfo(String decodedCertificate, boolean isEncoded) throws
            NoSuchAlgorithmException {

        int numberOfCertificates = StringUtils.countMatches(decodedCertificate, PEM_BEGIN_CERTFICATE);
        List<CertificateInfo> certificateInfoArrayList = new ArrayList<>();
        for (int ordinal = 1; ordinal <= numberOfCertificates; ordinal++) {
            String certificateVal;
            if (isEncoded) {
                certificateVal = Base64.getEncoder().encodeToString(IdentityApplicationManagementUtil.extractCertificate
                        (decodedCertificate, ordinal).getBytes(StandardCharsets.UTF_8));
            } else {
                certificateVal = IdentityApplicationManagementUtil.extractCertificate(decodedCertificate, ordinal).
                        replace(PEM_BEGIN_CERTFICATE, "").replace(
                                PEM_END_CERTIFICATE, "");
            }
            CertificateInfo certificateInfo = new CertificateInfo();
            String thumbPrint = IdentityApplicationManagementUtil.generateThumbPrint(certificateVal);

            certificateInfo.setThumbPrint(thumbPrint);
            certificateInfo.setCertValue(certificateVal);
            certificateInfoArrayList.add(certificateInfo);
        }
        return certificateInfoArrayList.toArray(new CertificateInfo[numberOfCertificates]);
    }*/

    /*private static CertificateInfo[] handleEncodedCertificate(String certificateValue) throws NoSuchAlgorithmException {

        String decodedCertificate;
        try {
            decodedCertificate = new String(Base64.getDecoder().decode(certificateValue));
        } catch (IllegalArgumentException ex) {
            // TODO Need to handle the exception handling in proper way.
            return createCertificateInfoForNoBeginCertificate(certificateValue);
        }
        if (StringUtils.isNotBlank(decodedCertificate) && !decodedCertificate.startsWith(PEM_BEGIN_CERTFICATE)) {
            // Handle certificates which are one time encoded but doesn't have BEGIN and END statement
            return createCertificateInfoForNoBeginCertificate(certificateValue);
        } else {
            return createEncodedCertificateInfo(decodedCertificate, true);
        }
    }*/

    /*private static CertificateInfo[] createCertificateInfoForNoBeginCertificate(String certificateValue) throws NoSuchAlgorithmException {

        String encodedCertVal = Base64.getEncoder().encodeToString(certificateValue.getBytes());
        String thumbPrint = IdentityApplicationManagementUtil.generateThumbPrint(encodedCertVal);
        List<CertificateInfo> certificateInfoList = new ArrayList<>();
        CertificateInfo certificateInfo = new CertificateInfo();
        certificateInfo.setThumbPrint(thumbPrint);
        certificateInfo.setCertValue(certificateValue);
        certificateInfoList.add(certificateInfo);
        return certificateInfoList.toArray(new CertificateInfo[1]);
    }*/
}