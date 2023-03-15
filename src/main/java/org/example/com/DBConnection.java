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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    /**
     * Static object of SQL connection.
     */
    private static Connection mysql_connection = null;
    private static Connection oracle_connection = null;

    /**
     * Create database connection if closed. Else return existing connection.
     * @param url url of database.
     * @param username username of database.
     * @param password password of database.
     * @return database connection.
     */
    public static Connection getOracleConnection(String url, String username, String password) throws SQLException {
        if (oracle_connection == null || oracle_connection.isClosed()) {
            try {
                oracle_connection = DriverManager.getConnection(url, username, password);
            } catch (SQLException e) {
                System.out.println("Unable to connect to oracle database.");
                System.out.println("Error: " + e.getMessage());
                return null;
            }
        }
        return oracle_connection;
    }

    /**
     * Create database connection if closed. Else return existing connection.
     * @param url url of database.
     * @param username username of database.
     * @param password password of database.
     * @return database connection.
     */
    public static Connection getMysqlConnection(String url, String username, String password) throws SQLException {
        if (mysql_connection == null || mysql_connection.isClosed()) {
            try {
                mysql_connection = DriverManager.getConnection(url, username, password);
            } catch (SQLException e) {
                System.out.println("Unable to connect to mysql database.");
                System.out.println("Error: " + e.getMessage());
                return null;
            }
        }
        return mysql_connection;
    }

    /**
     * Load JDBC driver.
     * @param driverLoacation location of JAR file.
     * @param jdbcConnectionClass Connection classs name.
     */
    public static void loadDBDriver(String driverLoacation, String jdbcConnectionClass) {
        File file = new File(driverLoacation);
        URL url = null;
        try {
            url = file.toURI().toURL();
        } catch (MalformedURLException e) {
            System.out.printf("Unable to open url.");
            System.out.println("Error: " + e.getMessage());
        }
        URLClassLoader ucl = new URLClassLoader(new URL[] {url});
        Driver driver = null;
        try {
            driver = (Driver) Class.forName(jdbcConnectionClass, true, ucl).newInstance();
        } catch (InstantiationException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (IllegalAccessException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
        }
        try {
            DriverManager.registerDriver(new DriverShim(driver));
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
