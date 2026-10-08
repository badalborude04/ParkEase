/*
    ================================================================
    ParkEase - Smart Parking Management Platform
    ================================================================

    Project Description:
    This project implements a console-based parking management
    platform using Core Java, OOP, JDBC and MySQL.

    Main Functionalities:
        1. Vehicle parking and ticket generation
        2. Vehicle exit and payment processing
        3. Multiple parking floors and gates
        4. Vehicle search and parking history
        5. Parking availability and display board
        6. Reservation and monthly pass management
        7. VIP, EV and handicapped parking support
        8. Revenue and statistics reports
        9. Lost ticket handling
       10. Admin login and audit logging

    Design Patterns Used:
        - Factory Pattern   : Vehicle object creation
        - Observer Pattern  : Parking display board updates
        - Strategy Pattern  : Parking, pricing and payment strategies
        - Singleton Pattern : Single ParkingLot object

    Database:
        MySQL database : parkingdb
        Connectivity   : JDBC

    Important:
        Database credentials are read from environment variables.
        Do not store real database passwords in source code or GitHub.

    ================================================================
*/

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/////////////////////////////////////////////////////////
// ParkingLot Automation System
// Marvellous Infosystems Style - Complete Feature Version
/////////////////////////////////////////////////////////

/////////////////////////////////////////////////////////
// Database Connection Class
// This class is responsible for creating and returning JDBC connections.
// It also initializes the parking database and required tables.
// Concepts : JDBC, Database Connectivity
/////////////////////////////////////////////////////////

class DatabaseConnection
{
    private static final String SERVER_URL = "jdbc:mysql://localhost:3306/";
    private static final String DATABASE_URL = "jdbc:mysql://localhost:3306/parkingdb";
    // Keep credentials outside source code for GitHub safety.
    // Set environment variables before running:
    // PARKING_DB_USERNAME=root
    // PARKING_DB_PASSWORD=your_mysql_password
    private static final String USERNAME = System.getenv().getOrDefault("PARKING_DB_USERNAME", "root");
    private static final String PASSWORD = System.getenv("PARKING_DB_PASSWORD");

    private static void validateCredentials() throws SQLException
    {
        if(PASSWORD == null || PASSWORD.isEmpty())
        {
            throw new SQLException("PARKING_DB_PASSWORD environment variable is not set");
        }
    }

    // Returns a JDBC connection to the MySQL server (without selecting a database)
    public static Connection getServerConnection() throws SQLException
    {
        validateCredentials();
        return DriverManager.getConnection(SERVER_URL, USERNAME, PASSWORD);
    }

    // Returns a JDBC connection to the parkingdb database
    public static Connection getConnection() throws SQLException
    {
        return DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
    }

    // Creates the database and all required tables if they do not already exist
    public static void initializeDatabase() throws SQLException
    {
        try(Connection connection = getServerConnection();
            Statement statement = connection.createStatement())
        {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS parkingdb");
        }

        try(Connection connection = getConnection();
            Statement statement = connection.createStatement())
        {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS parking_floors (" +
                    "floor_id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "floor_number INT UNIQUE NOT NULL)");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS parking_spots (" +
                    "spot_id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "floor_id INT NOT NULL, " +
                    "spot_number INT NOT NULL, " +
                    "spot_type VARCHAR(30) NOT NULL, " +
                    "is_occupied BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "vehicle_number VARCHAR(50), " +
                    "is_vip BOOLEAN DEFAULT FALSE, " +
                    "is_ev BOOLEAN DEFAULT FALSE, " +
                    "is_handicapped BOOLEAN DEFAULT FALSE, " +
                    "UNIQUE(floor_id,spot_number), " +
                    "FOREIGN KEY(floor_id) REFERENCES parking_floors(floor_id))");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS vehicles (" +
                    "vehicle_id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "vehicle_number VARCHAR(50) UNIQUE NOT NULL, " +
                    "vehicle_type VARCHAR(20) NOT NULL, " +
                    "is_vip BOOLEAN DEFAULT FALSE, " +
                    "is_ev BOOLEAN DEFAULT FALSE, " +
                    "is_handicapped BOOLEAN DEFAULT FALSE)");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS parking_tickets (" +
                    "ticket_number INT PRIMARY KEY, vehicle_number VARCHAR(50) NOT NULL, " +
                    "floor_number INT NOT NULL, spot_number INT NOT NULL, " +
                    "entry_time DATETIME NOT NULL, exit_time DATETIME, " +
                    "ticket_status VARCHAR(20) NOT NULL, parking_hours BIGINT DEFAULT 0, " +
                    "final_amount DOUBLE DEFAULT 0, is_vip BOOLEAN DEFAULT FALSE, " +
                    "gate_number INT DEFAULT 1)");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS payments (" +
                    "payment_id INT PRIMARY KEY AUTO_INCREMENT, ticket_number INT NOT NULL, " +
                    "payment_type VARCHAR(20) NOT NULL, amount DOUBLE NOT NULL, " +
                    "payment_time DATETIME NOT NULL, payment_status VARCHAR(20) NOT NULL)");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS reservations (" +
                    "reservation_id INT PRIMARY KEY AUTO_INCREMENT, vehicle_number VARCHAR(50) NOT NULL, " +
                    "vehicle_type VARCHAR(20) NOT NULL, reservation_time DATETIME NOT NULL, " +
                    "status VARCHAR(20) NOT NULL)");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS monthly_passes (" +
                    "pass_id INT PRIMARY KEY AUTO_INCREMENT, vehicle_number VARCHAR(50) UNIQUE NOT NULL, " +
                    "start_date DATE NOT NULL, end_date DATE NOT NULL, amount DOUBLE NOT NULL DEFAULT 0, " +
                    "status VARCHAR(20) NOT NULL)");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS admin_users (" +
                    "admin_id INT PRIMARY KEY AUTO_INCREMENT, username VARCHAR(50) UNIQUE NOT NULL, " +
                    "password VARCHAR(100) NOT NULL)");

            statement.executeUpdate("INSERT IGNORE INTO admin_users(username,password) VALUES('admin','admin123')");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS audit_logs (" +
                    "log_id INT PRIMARY KEY AUTO_INCREMENT, action VARCHAR(100) NOT NULL, " +
                    "details VARCHAR(500), log_time DATETIME NOT NULL)");
        }
    }

    public static void ensureColumn(String table, String column, String definition) throws SQLException
    {
        String check = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                       "WHERE TABLE_SCHEMA='parkingdb' AND TABLE_NAME=? AND COLUMN_NAME=?";
        try(Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(check))
        {
            statement.setString(1, table);
            statement.setString(2, column);
            try(ResultSet result = statement.executeQuery())
            {
                result.next();
                if(result.getInt(1) == 0)
                {
                    try(Statement alter = connection.createStatement())
                    {
                        alter.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
                    }
                }
            }
        }
    }

    public static void ensureCompatibility() throws SQLException
    {
        ensureColumn("vehicles", "is_vip", "BOOLEAN DEFAULT FALSE");
        ensureColumn("vehicles", "is_ev", "BOOLEAN DEFAULT FALSE");
        ensureColumn("vehicles", "is_handicapped", "BOOLEAN DEFAULT FALSE");
        ensureColumn("parking_spots", "vehicle_number", "VARCHAR(50)");
        ensureColumn("parking_spots", "is_vip", "BOOLEAN DEFAULT FALSE");
        ensureColumn("parking_spots", "is_ev", "BOOLEAN DEFAULT FALSE");
        ensureColumn("parking_spots", "is_handicapped", "BOOLEAN DEFAULT FALSE");
        ensureColumn("parking_tickets", "is_vip", "BOOLEAN DEFAULT FALSE");
        ensureColumn("parking_tickets", "gate_number", "INT DEFAULT 1");
        ensureColumn("monthly_passes", "amount", "DOUBLE NOT NULL DEFAULT 0");

        // Ensure admin table exists for older databases.
        try(Connection connection=getConnection(); Statement statement=connection.createStatement())
        {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS admin_users (" +
                    "admin_id INT PRIMARY KEY AUTO_INCREMENT, username VARCHAR(50) UNIQUE NOT NULL, " +
                    "password VARCHAR(100) NOT NULL)");
            statement.executeUpdate("INSERT IGNORE INTO admin_users(username,password) VALUES('admin','admin123')");
        }
    }
}

/////////////////////////////////////////////////////////
// Parking Database Class
// This class contains all JDBC CRUD operations used by the application.
// Keeping database operations here separates persistence logic from business logic.
// Concepts : JDBC CRUD Operations
/////////////////////////////////////////////////////////

class ParkingDatabase
{
    /////////////////////////////////////////////////////////
    // Save Floor Information
    /////////////////////////////////////////////////////////

    // Saves floor information using INSERT/UPDATE logic
    public static void saveFloor(ParkingFloor floor) throws SQLException
    {
        String query = "INSERT INTO parking_floors(floor_number) VALUES(?) " +
                       "ON DUPLICATE KEY UPDATE floor_number=VALUES(floor_number)";
        try(Connection connection = DatabaseConnection.getConnection();
            PreparedStatement statement = connection.prepareStatement(query))
        {
            statement.setInt(1, floor.getFloorNumber());
            statement.executeUpdate();
        }
    }

    public static int getFloorId(int floorNumber) throws SQLException
    {
        String query = "SELECT floor_id FROM parking_floors WHERE floor_number=?";
        try(Connection connection = DatabaseConnection.getConnection();
            PreparedStatement statement = connection.prepareStatement(query))
        {
            statement.setInt(1, floorNumber);
            try(ResultSet result = statement.executeQuery())
            {
                if(result.next()) return result.getInt(1);
            }
        }
        throw new SQLException("Floor not found in database");
    }

    // Saves or updates a parking spot and its current occupancy state
    public static void saveSpot(ParkingFloor floor, ParkingSpot spot) throws SQLException
    {
        saveFloor(floor);
        int floorId = getFloorId(floor.getFloorNumber());
        String query = "INSERT INTO parking_spots(floor_id,spot_number,spot_type,is_occupied,vehicle_number,is_vip,is_ev,is_handicapped) " +
                       "VALUES(?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE spot_type=VALUES(spot_type)," +
                       "is_vip=VALUES(is_vip),is_ev=VALUES(is_ev),is_handicapped=VALUES(is_handicapped)";
        try(Connection connection = DatabaseConnection.getConnection();
            PreparedStatement statement = connection.prepareStatement(query))
        {
            statement.setInt(1, floorId);
            statement.setInt(2, spot.getSpotNumber());
            statement.setString(3, spot.getSpotType().name());
            statement.setBoolean(4, spot.isOccupied());
            if(spot.getVehicle()==null) statement.setNull(5, Types.VARCHAR);
            else statement.setString(5, spot.getVehicle().getVehicleNumber());
            statement.setBoolean(6, spot.isVip());
            statement.setBoolean(7, spot.isEv());
            statement.setBoolean(8, spot.isHandicapped());
            statement.executeUpdate();
        }
    }

    // Stores vehicle information in the vehicles table
    public static void saveVehicle(Vehicle vehicle) throws SQLException
    {
        String query = "INSERT INTO vehicles(vehicle_number,vehicle_type,is_vip,is_ev,is_handicapped) VALUES(?,?,?,?,?) " +
                       "ON DUPLICATE KEY UPDATE vehicle_type=VALUES(vehicle_type),is_vip=VALUES(is_vip)," +
                       "is_ev=VALUES(is_ev),is_handicapped=VALUES(is_handicapped)";
        try(Connection connection = DatabaseConnection.getConnection();
            PreparedStatement statement = connection.prepareStatement(query))
        {
            statement.setString(1, vehicle.getVehicleNumber());
            statement.setString(2, vehicle.getVehicleType().name());
            statement.setBoolean(3, vehicle.isVip());
            statement.setBoolean(4, vehicle.isEv());
            statement.setBoolean(5, vehicle.isHandicapped());
            statement.executeUpdate();
        }
    }

    public static void saveTicket(ParkingTicket ticket, int gateNumber) throws SQLException
    {
        String query = "INSERT INTO parking_tickets(ticket_number,vehicle_number,floor_number,spot_number,entry_time," +
                       "ticket_status,parking_hours,final_amount,is_vip,gate_number) VALUES(?,?,?,?,?,?,?,?,?,?)";
        try(Connection connection = DatabaseConnection.getConnection();
            PreparedStatement statement = connection.prepareStatement(query))
        {
            statement.setInt(1,ticket.getTicketNumber());
            statement.setString(2,ticket.getVehicle().getVehicleNumber());
            statement.setInt(3,ticket.getFloor().getFloorNumber());
            statement.setInt(4,ticket.getSpot().getSpotNumber());
            statement.setTimestamp(5,Timestamp.valueOf(ticket.getEntryTime()));
            statement.setString(6,ticket.getStatus().name());
            statement.setLong(7,0);
            statement.setDouble(8,0);
            statement.setBoolean(9,ticket.getVehicle().isVip());
            statement.setInt(10,gateNumber);
            statement.executeUpdate();
        }
    }

    // Updates ticket status, exit time, parking duration and final amount
    public static void updateTicketOnExit(ParkingTicket ticket) throws SQLException
    {
        String query = "UPDATE parking_tickets SET exit_time=?,ticket_status=?,parking_hours=?,final_amount=? WHERE ticket_number=?";
        try(Connection connection=DatabaseConnection.getConnection(); PreparedStatement statement=connection.prepareStatement(query))
        {
            statement.setTimestamp(1,Timestamp.valueOf(ticket.getExitTime()));
            statement.setString(2,ticket.getStatus().name());
            statement.setLong(3,ticket.calculateHours());
            statement.setDouble(4,ticket.getFinalAmount());
            statement.setInt(5,ticket.getTicketNumber());
            statement.executeUpdate();
        }
    }

    // Stores successful payment information in the payments table
    public static void savePayment(int ticketNumber,String paymentType,double amount) throws SQLException
    {
        String query="INSERT INTO payments(ticket_number,payment_type,amount,payment_time,payment_status) VALUES(?,?,?,?,?)";
        try(Connection connection=DatabaseConnection.getConnection(); PreparedStatement statement=connection.prepareStatement(query))
        {
            statement.setInt(1,ticketNumber); statement.setString(2,paymentType); statement.setDouble(3,amount);
            statement.setTimestamp(4,Timestamp.valueOf(LocalDateTime.now())); statement.setString(5,"SUCCESS");
            statement.executeUpdate();
        }
    }

    // Synchronizes the in-memory parking spot state with MySQL
    public static void updateSpotStatus(ParkingFloor floor,ParkingSpot spot) throws SQLException
    {
        int floorId=getFloorId(floor.getFloorNumber());
        String query="UPDATE parking_spots SET is_occupied=?,vehicle_number=? WHERE floor_id=? AND spot_number=?";
        try(Connection connection=DatabaseConnection.getConnection(); PreparedStatement statement=connection.prepareStatement(query))
        {
            statement.setBoolean(1,spot.isOccupied());
            if(spot.getVehicle()==null) statement.setNull(2,Types.VARCHAR); else statement.setString(2,spot.getVehicle().getVehicleNumber());
            statement.setInt(3,floorId); statement.setInt(4,spot.getSpotNumber()); statement.executeUpdate();
        }
    }

    public static String getPaymentType(PaymentStrategy strategy)
    {
        if(strategy instanceof CashPayment) return "CASH";
        if(strategy instanceof UPIPayment) return "UPI";
        if(strategy instanceof CardPayment) return "CARD";
        return "UNKNOWN";
    }

    public static void showAvailability() throws SQLException
    {
        String query="SELECT COUNT(*),SUM(CASE WHEN is_occupied=TRUE THEN 1 ELSE 0 END),SUM(CASE WHEN is_occupied=FALSE THEN 1 ELSE 0 END) FROM parking_spots";
        try(Connection connection=DatabaseConnection.getConnection(); Statement statement=connection.createStatement(); ResultSet result=statement.executeQuery(query))
        {
            if(result.next())
            {
                System.out.println("\n---------------------------------");
                System.out.println("------ Parking Availability -----");
                System.out.println("---------------------------------");
                System.out.println("Total Spots     : "+result.getInt(1));
                System.out.println("Occupied Spots  : "+result.getInt(2));
                System.out.println("Available Spots : "+result.getInt(3));
                System.out.println("---------------------------------");
            }
        }
    }

    public static void searchVehicle(String number) throws SQLException
    {
        String query="SELECT * FROM parking_tickets WHERE vehicle_number=? ORDER BY entry_time DESC LIMIT 1";
        try(Connection connection=DatabaseConnection.getConnection(); PreparedStatement statement=connection.prepareStatement(query))
        {
            statement.setString(1,number);
            try(ResultSet result=statement.executeQuery())
            {
                if(result.next())
                {
                    System.out.println("\n---------- Vehicle Search ----------");
                    System.out.println("Vehicle Number : "+result.getString("vehicle_number"));
                    System.out.println("Ticket Number  : "+result.getInt("ticket_number"));
                    System.out.println("Floor Number   : "+result.getInt("floor_number"));
                    System.out.println("Spot Number    : "+result.getInt("spot_number"));
                    System.out.println("Entry Time     : "+result.getTimestamp("entry_time"));
                    System.out.println("Exit Time      : "+result.getTimestamp("exit_time"));
                    System.out.println("Status         : "+result.getString("ticket_status"));
                    System.out.println("Parking Hours  : "+result.getLong("parking_hours"));
                    System.out.println("Final Amount   : Rs. "+result.getDouble("final_amount"));
                    System.out.println("------------------------------------");
                }
                else System.out.println("Vehicle record not found");
            }
        }
    }

    public static void showHistory(String number) throws SQLException
    {
        String query="SELECT ticket_number,entry_time,exit_time,ticket_status,parking_hours,final_amount FROM parking_tickets WHERE vehicle_number=? ORDER BY entry_time DESC";
        try(Connection connection=DatabaseConnection.getConnection(); PreparedStatement statement=connection.prepareStatement(query))
        {
            statement.setString(1,number);
            try(ResultSet result=statement.executeQuery())
            {
                System.out.println("\n------------- Parking History -------------");
                boolean found=false;
                while(result.next())
                {
                    found=true;
                    System.out.println("Ticket : "+result.getInt(1)+" | Entry : "+result.getTimestamp(2)+" | Exit : "+result.getTimestamp(3)+" | Status : "+result.getString(4)+" | Hours : "+result.getLong(5)+" | Amount : Rs."+result.getDouble(6));
                }
                if(!found) System.out.println("No parking history found");
                System.out.println("--------------------------------------------");
            }
        }
    }

    public static void showRevenue() throws SQLException
    {
        String query="SELECT COUNT(*),COALESCE(SUM(final_amount),0),COALESCE(AVG(final_amount),0) FROM parking_tickets WHERE ticket_status='CLOSED'";
        try(Connection connection=DatabaseConnection.getConnection(); Statement statement=connection.createStatement(); ResultSet result=statement.executeQuery(query))
        {
            result.next();
            System.out.println("\n------------- Revenue Report -------------");
            System.out.println("Completed Tickets : "+result.getInt(1));
            System.out.println("Total Revenue    : Rs. "+result.getDouble(2));
            System.out.println("Average Revenue  : Rs. "+result.getDouble(3));
            System.out.println("------------------------------------------");
        }
    }

    public static void showStatistics() throws SQLException
    {
        String query="SELECT COUNT(*),SUM(CASE WHEN ticket_status='ACTIVE' THEN 1 ELSE 0 END),SUM(CASE WHEN ticket_status='CLOSED' THEN 1 ELSE 0 END) FROM parking_tickets";
        try(Connection connection=DatabaseConnection.getConnection(); Statement statement=connection.createStatement(); ResultSet result=statement.executeQuery(query))
        {
            result.next();
            System.out.println("\n------------- Parking Statistics -------------");
            System.out.println("Total Tickets : "+result.getInt(1));
            System.out.println("Active Tickets: "+result.getInt(2));
            System.out.println("Closed Tickets: "+result.getInt(3));
            System.out.println("-----------------------------------------------");
        }
    }

    public static void showPaymentHistory() throws SQLException
    {
        String query="SELECT ticket_number,payment_type,amount,payment_time,payment_status FROM payments ORDER BY payment_time DESC";
        try(Connection connection=DatabaseConnection.getConnection(); Statement statement=connection.createStatement(); ResultSet result=statement.executeQuery(query))
        {
            System.out.println("\n------------- Payment History -------------");
            while(result.next()) System.out.println("Ticket : "+result.getInt(1)+" | Type : "+result.getString(2)+" | Amount : Rs."+result.getDouble(3)+" | Time : "+result.getTimestamp(4)+" | Status : "+result.getString(5));
            System.out.println("-------------------------------------------");
        }
    }

    public static void saveReservation(String number,VehicleType type) throws SQLException
    {
        String query="INSERT INTO reservations(vehicle_number,vehicle_type,reservation_time,status) VALUES(?,?,?,?)";
        try(Connection connection=DatabaseConnection.getConnection(); PreparedStatement statement=connection.prepareStatement(query))
        {
            statement.setString(1,number); statement.setString(2,type.name()); statement.setTimestamp(3,Timestamp.valueOf(LocalDateTime.now())); statement.setString(4,"ACTIVE"); statement.executeUpdate();
        }
    }

    public static void showReservations() throws SQLException
    {
        String query="SELECT reservation_id,vehicle_number,vehicle_type,reservation_time,status FROM reservations ORDER BY reservation_id DESC";
        try(Connection connection=DatabaseConnection.getConnection(); Statement statement=connection.createStatement(); ResultSet result=statement.executeQuery(query))
        {
            System.out.println("\n------------- Reservations -------------");
            while(result.next()) System.out.println("ID : "+result.getInt(1)+" | Vehicle : "+result.getString(2)+" | Type : "+result.getString(3)+" | Time : "+result.getTimestamp(4)+" | Status : "+result.getString(5));
            System.out.println("----------------------------------------");
        }
    }

    public static void saveMonthlyPass(String number) throws SQLException
    {
        double amount=500.0;

        String query="INSERT INTO monthly_passes(vehicle_number,start_date,end_date,amount,status) " +
                     "VALUES(?,CURRENT_DATE,DATE_ADD(CURRENT_DATE,INTERVAL 30 DAY),?, 'ACTIVE') " +
                     "ON DUPLICATE KEY UPDATE end_date=DATE_ADD(CURRENT_DATE,INTERVAL 30 DAY),amount=?,status='ACTIVE'";

        try(Connection connection=DatabaseConnection.getConnection();
            PreparedStatement statement=connection.prepareStatement(query))
        {
            statement.setString(1,number);
            statement.setDouble(2,amount);
            statement.setDouble(3,amount);
            statement.executeUpdate();
        }
    }

    public static boolean hasMonthlyPass(String number) throws SQLException
    {
        String query="SELECT COUNT(*) FROM monthly_passes WHERE vehicle_number=? AND status='ACTIVE' AND end_date>=CURRENT_DATE";
        try(Connection connection=DatabaseConnection.getConnection(); PreparedStatement statement=connection.prepareStatement(query))
        { statement.setString(1,number); try(ResultSet result=statement.executeQuery()){ result.next(); return result.getInt(1)>0; } }
    }

    public static boolean hasColumn(String table,String column) throws SQLException
    {
        String query="SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='parkingdb' AND TABLE_NAME=? AND COLUMN_NAME=?";
        try(Connection connection=DatabaseConnection.getConnection(); PreparedStatement statement=connection.prepareStatement(query))
        {
            statement.setString(1,table); statement.setString(2,column);
            try(ResultSet result=statement.executeQuery())
            {
                result.next();
                return result.getInt(1)>0;
            }
        }
    }

    public static void audit(String action,String vehicle,String details) throws SQLException
    {
        // Supports both the current schema and older project databases.
        if(hasColumn("audit_logs","action_name") && hasColumn("audit_logs","vehicle_number") && hasColumn("audit_logs","action_time"))
        {
            String query="INSERT INTO audit_logs(action_name,vehicle_number,action_time,details) VALUES(?,?,?,?)";
            try(Connection connection=DatabaseConnection.getConnection(); PreparedStatement statement=connection.prepareStatement(query))
            {
                statement.setString(1,action); statement.setString(2,vehicle);
                statement.setTimestamp(3,Timestamp.valueOf(LocalDateTime.now())); statement.setString(4,details);
                statement.executeUpdate();
            }
        }
        else
        {
            String query="INSERT INTO audit_logs(action,details,log_time) VALUES(?,?,?)";
            try(Connection connection=DatabaseConnection.getConnection(); PreparedStatement statement=connection.prepareStatement(query))
            {
                statement.setString(1,action);
                statement.setString(2,"Vehicle : "+vehicle+" | "+details);
                statement.setTimestamp(3,Timestamp.valueOf(LocalDateTime.now()));
                statement.executeUpdate();
            }
        }
    }

    public static void showAuditLogs() throws SQLException
    {
        boolean legacy=hasColumn("audit_logs","action_name") && hasColumn("audit_logs","vehicle_number") && hasColumn("audit_logs","action_time");

        System.out.println("\n------------- Audit Log -------------");
        if(legacy)
        {
            String query="SELECT action_name,vehicle_number,action_time,details FROM audit_logs ORDER BY action_time DESC";
            try(Connection connection=DatabaseConnection.getConnection(); Statement statement=connection.createStatement(); ResultSet result=statement.executeQuery(query))
            {
                while(result.next())
                {
                    System.out.println("Time    : "+result.getTimestamp("action_time"));
                    System.out.println("Action  : "+result.getString("action_name"));
                    System.out.println("Details : Vehicle : "+result.getString("vehicle_number")+" | "+result.getString("details"));
                    System.out.println("-------------------------------------");
                }
            }
        }
        else
        {
            String query="SELECT action,details,log_time FROM audit_logs ORDER BY log_time DESC";
            try(Connection connection=DatabaseConnection.getConnection(); Statement statement=connection.createStatement(); ResultSet result=statement.executeQuery(query))
            {
                while(result.next())
                {
                    System.out.println("Time    : "+result.getTimestamp("log_time"));
                    System.out.println("Action  : "+result.getString("action"));
                    System.out.println("Details : "+result.getString("details"));
                    System.out.println("-------------------------------------");
                }
            }
        }
    }

    public static boolean validateAdmin(String username,String password) throws SQLException
    {
        String query="SELECT COUNT(*) FROM admin_users WHERE username=? AND password=?";

        try(Connection connection=DatabaseConnection.getConnection();
            PreparedStatement statement=connection.prepareStatement(query))
        {
            statement.setString(1,username);
            statement.setString(2,password);

            try(ResultSet result=statement.executeQuery())
            {
                result.next();
                return result.getInt(1)>0;
            }
        }
    }
}

/////////////////////////////////////////////////////////
// Step 1 : Create Enums
//
// Purpose:
// Enums define a fixed set of values required throughout
// the parking application.
//
// VehicleType:
//     Represents the category of vehicle entering the parking lot.
//
// SpotType:
//     Represents the category of parking spot available.
//
// TicketStatus:
//     Represents the current state of a parking ticket.
//
// Why enum is used:
//     It provides type safety and prevents invalid values.
//
// Java Concept:
//     Enumeration and type-safe constants
/////////////////////////////////////////////////////////

// Represents the different types of vehicles supported by the project
enum VehicleType
{
    BIKE,
    CAR,
    TRUCK
}

// Represents different types of parking spots
enum SpotType
{
    BIKE,
    CAR,
    TRUCK,
    VIP,
    EV,
    HANDICAPPED
}

// Represents the current state of parking ticket
enum TicketStatus
{
    ACTIVE,
    CLOSED
}

/////////////////////////////////////////////////////////
// Step 2 : Create Vehicle Class Hierarchy
//
// Purpose:
// The Vehicle class contains common properties and behaviour
// shared by all vehicle types.
//
// Vehicle is abstract because a generic Vehicle object should
// not be created directly.
//
// Hierarchy:
//     Vehicle
//       |-- Bike
//       |-- Car
//       |-- Truck
//
// Concepts demonstrated:
//     1. Abstraction
//     2. Encapsulation
//     3. Inheritance
//     4. Polymorphism
//
// The display() method is overridden by every concrete vehicle
// class according to its own implementation.
/////////////////////////////////////////////////////////

abstract class Vehicle
{
    private String vehicleNumber;
    private VehicleType vehicleType;
    private boolean vip;
    private boolean ev;
    private boolean handicapped;

    // Parametrised constructor
    public Vehicle(String vehicleNumber,VehicleType vehicleType)
    {
        this.vehicleNumber=vehicleNumber;
        this.vehicleType=vehicleType;
    }

    // Getter methods
    public VehicleType getVehicleType()
    {
        return this.vehicleType;
    }

    public String getVehicleNumber()
    {
        return this.vehicleNumber;
    }

    public boolean isVip()
    {
        return this.vip;
    }

    public boolean isEv()
    {
        return this.ev;
    }

    public boolean isHandicapped()
    {
        return this.handicapped;
    }

    // Setter methods
    public void setVip(boolean value)
    {
        this.vip=value;
    }

    public void setEv(boolean value)
    {
        this.ev=value;
    }

    public void setHandicapped(boolean value)
    {
        this.handicapped=value;
    }

    // Every concrete vehicle provides its own implementation
    public abstract void display();
}

class Bike extends Vehicle
{
    public Bike(String number){ super(number,VehicleType.BIKE); }
    public void display(){ System.out.println("Bike : "+getVehicleNumber()); }
}
class Car extends Vehicle
{
    public Car(String number){ super(number,VehicleType.CAR); }
    public void display(){ System.out.println("Car : "+getVehicleNumber()); }
}
class Truck extends Vehicle
{
    public Truck(String number){ super(number,VehicleType.TRUCK); }
    public void display(){ System.out.println("Truck : "+getVehicleNumber()); }
}

/////////////////////////////////////////////////////////
// Step 3 : Create VehicleFactory Class
//
// Purpose:
// VehicleFactory centralizes the creation of Vehicle objects.
//
// Instead of creating Bike, Car or Truck objects directly,
// the controller requests the required object from the factory.
//
// Flow:
//     VehicleType
//         |
//         v
//     VehicleFactory
//         |
//         v
//     Concrete Vehicle Object
//
// Benefit:
// Object creation logic remains in one place and the controller
// does not depend heavily on concrete vehicle classes.
//
// Design Pattern:
//     Factory Pattern
/////////////////////////////////////////////////////////

class VehicleFactory
{
    public static Vehicle createVehicle(VehicleType type,String number)
    {
        switch(type)
        {
            case BIKE: return new Bike(number);
            case CAR: return new Car(number);
            case TRUCK: return new Truck(number);
            default: throw new IllegalArgumentException("Invalid Vehicle type");
        }
    }
}

/////////////////////////////////////////////////////////
// Step 4 : Create ParkingSpot Hierarchy
//
// Purpose:
// A parking lot contains different categories of parking spots.
//
// Hierarchy:
//     ParkingSpot
//       |-- BikeSpot
//       |-- CarSpot
//       |-- TruckSpot
//
// Common responsibilities:
//     - Store spot number and type
//     - Maintain occupied/free state
//     - Store the parked vehicle
//     - Release the spot after vehicle exit
//
// The hierarchy demonstrates abstraction, inheritance,
// encapsulation and polymorphism.
/////////////////////////////////////////////////////////

abstract class ParkingSpot
{
    private int spotNumber;
    private SpotType spotType;
    private boolean occupied;
    private Vehicle vehicle;
    private boolean vip;
    private boolean ev;
    private boolean handicapped;

    public ParkingSpot(int spotNumber,SpotType spotType)
    {
        this.spotNumber=spotNumber; this.spotType=spotType;
    }

    public int getSpotNumber(){ return spotNumber; }
    public SpotType getSpotType(){ return spotType; }
    public boolean isOccupied(){ return occupied; }
    public Vehicle getVehicle(){ return vehicle; }
    public boolean isVip(){ return vip; }
    public boolean isEv(){ return ev; }
    public boolean isHandicapped(){ return handicapped; }
    public void setVip(boolean value){ vip=value; }
    public void setEv(boolean value){ ev=value; }
    public void setHandicapped(boolean value){ handicapped=value; }

    public void parkVehicle(Vehicle vehicle)
    {
        if(occupied) throw new RuntimeException("Parking spot is already occupied");
        this.vehicle=vehicle; this.occupied=true;
    }

    public Vehicle removeVehicle()
    {
        if(!occupied) throw new RuntimeException("Parking spot is already empty");
        Vehicle temp=vehicle; vehicle=null; occupied=false; return temp;
    }

    public abstract boolean canFitVehicle(Vehicle vehicle);

    public void display()
    {
        System.out.println("Spot : "+spotNumber+" ["+spotType+"]"+
                           " [VIP="+vip+", EV="+ev+", HANDICAPPED="+handicapped+"]");
        if(occupied) System.out.println("Occupied by : "+vehicle.getVehicleNumber());
        else System.out.println("Spot is available");
    }
}

class BikeSpot extends ParkingSpot
{
    public BikeSpot(int number){ super(number,SpotType.BIKE); }
    public boolean canFitVehicle(Vehicle vehicle){ return vehicle.getVehicleType()==VehicleType.BIKE; }
}
class CarSpot extends ParkingSpot
{
    public CarSpot(int number){ super(number,SpotType.CAR); }
    public boolean canFitVehicle(Vehicle vehicle){ return vehicle.getVehicleType()==VehicleType.CAR; }
}
class TruckSpot extends ParkingSpot
{
    public TruckSpot(int number){ super(number,SpotType.TRUCK); }
    public boolean canFitVehicle(Vehicle vehicle){ return vehicle.getVehicleType()==VehicleType.TRUCK; }
}

/////////////////////////////////////////////////////////
// Step 5 : Create ParkingObserver Class
//
// Purpose:
// Parking availability should be reflected automatically on
// the parking display whenever a spot changes its state.
//
// Observer Pattern:
//     ParkingFloor acts as the subject.
//     ParkingDisplayBoard acts as the observer.
//
// Flow:
//     ParkingFloor
//          |
//          | state changed
//          v
//     Observer notification
//          |
//          v
//     ParkingDisplayBoard
//
// Benefit:
// Parking-floor logic remains independent from display logic.
//
// Design Pattern:
//     Observer Pattern
/////////////////////////////////////////////////////////

interface ParkingObserver { void update(); }

/////////////////////////////////////////////////////////
// Step 6 : Create ParkingFloor Class
//
// Purpose:
// ParkingFloor represents one complete floor of the parking lot.
//
// Responsibilities:
//     - Maintain parking spots
//     - Add new spots
//     - Find suitable available spots
//     - Occupy a selected spot
//     - Release a spot after vehicle exit
//     - Notify observers about availability changes
//
// A ParkingLot can contain multiple ParkingFloor objects.
//
// Example:
//     ParkingLot
//       |-- Floor 1 -> Spot 101, 102...
//       |-- Floor 2 -> Spot 201, 202...
//
// This class contains the main floor-level parking operations.
/////////////////////////////////////////////////////////

class ParkingFloor
{
    private int floorNumber;
    private List<ParkingSpot> parkingSpots;
    private List<ParkingObserver> observers;

    public ParkingFloor(int floorNumber)
    {
        this.floorNumber=floorNumber; parkingSpots=new ArrayList<>(); observers=new ArrayList<>();
    }

    public int getFloorNumber(){ return floorNumber; }
    public void addParkingSpot(ParkingSpot spot){ parkingSpots.add(spot); }
    public List<ParkingSpot> getParkingSpots(){ return parkingSpots; }
    public void addObserver(ParkingObserver observer){ observers.add(observer); }

    private void notifyObservers(){ for(ParkingObserver observer:observers) observer.update(); }

    public ParkingSpot findAvailableSpot(Vehicle vehicle)
    {
        for(ParkingSpot spot:parkingSpots)
            if(!spot.isOccupied() && spot.canFitVehicle(vehicle)) return spot;
        return null;
    }

    public ParkingSpot findSpecialSpot(Vehicle vehicle)
    {
        for(ParkingSpot spot:parkingSpots)
        {
            if(spot.isOccupied()) continue;
            if(vehicle.isVip() && spot.isVip() && spot.canFitVehicle(vehicle)) return spot;
            if(vehicle.isEv() && spot.isEv() && spot.canFitVehicle(vehicle)) return spot;
            if(vehicle.isHandicapped() && spot.isHandicapped() && spot.canFitVehicle(vehicle)) return spot;
        }
        return null;
    }

    public void occupySpot(ParkingSpot spot,Vehicle vehicle){ spot.parkVehicle(vehicle); notifyObservers(); }
    public void releaseSpot(ParkingSpot spot){ spot.removeVehicle(); notifyObservers(); }

    public int getAvailableleCount(SpotType type)
    {
        int count=0;
        for(ParkingSpot spot:parkingSpots) if(spot.getSpotType()==type && !spot.isOccupied()) count++;
        return count;
    }

    public void displayFloor()
    {
        System.out.println("\nFloor : "+floorNumber);
        for(ParkingSpot spot:parkingSpots) spot.display();
    }
}

/////////////////////////////////////////////////////////
// Step 7 : Create ParkingDisplayBoard
//
// Purpose:
// ParkingDisplayBoard shows the current parking availability
// for a particular floor.
//
// It receives updates through the Observer Pattern whenever
// parking availability changes.
//
// Typical information:
//     - Available bike spots
//     - Available car spots
//     - Available truck spots
//     - Floor-level availability
//
// Relationship:
//     ParkingFloor  --->  ParkingDisplayBoard
//       Subject             Observer
//
// Benefit:
// Display responsibilities remain separate from parking logic.
//
// Design Pattern:
//     Observer Pattern
/////////////////////////////////////////////////////////

class ParkingDisplayBoard implements ParkingObserver
{
    private ParkingFloor floor;
    public ParkingDisplayBoard(ParkingFloor floor){ this.floor=floor; }
    public void update()
    {
        System.out.println("\n--------- Display Board ---------");
        System.out.println("Floor : "+floor.getFloorNumber());
        System.out.println("Available Bike spots : "+floor.getAvailableleCount(SpotType.BIKE));
        System.out.println("Available Car spots : "+floor.getAvailableleCount(SpotType.CAR));
        System.out.println("Available Truck spots : "+floor.getAvailableleCount(SpotType.TRUCK));
        System.out.println("---------------------------------");
    }
}

/////////////////////////////////////////////////////////
// Step 8 : Create ParkingStrategy Class
//
// Purpose:
// ParkingStrategy defines the algorithm used to select a
// suitable parking spot for a vehicle.
//
// Possible strategies include:
//     - First available spot
//     - Nearest available spot
//     - Vehicle-type based selection
//
// ParkingLot only uses the common strategy interface and does
// not need to know how the actual selection is implemented.
//
// Benefit:
// Parking allocation logic can be changed independently.
//
// Design Pattern:
//     Strategy Pattern
/////////////////////////////////////////////////////////

interface ParkingStrategy { ParkingSpot findSpot(List<ParkingFloor> floors,Vehicle vehicle); }

class FirstAvailableParkingStrategy implements ParkingStrategy
{
    public ParkingSpot findSpot(List<ParkingFloor> floors,Vehicle vehicle)
    {
        for(ParkingFloor floor:floors)
        {
            ParkingSpot special=floor.findSpecialSpot(vehicle);
            if(special!=null) return special;
            ParkingSpot spot=floor.findAvailableSpot(vehicle);
            if(spot!=null) return spot;
        }
        return null;
    }
}

class NearestAvailableParkingStrategy implements ParkingStrategy
{
    public ParkingSpot findSpot(List<ParkingFloor> floors,Vehicle vehicle)
    {
        for(ParkingFloor floor:floors)
        {
            ParkingSpot special=floor.findSpecialSpot(vehicle);
            if(special!=null) return special;
        }
        for(ParkingFloor floor:floors)
        {
            ParkingSpot spot=floor.findAvailableSpot(vehicle);
            if(spot!=null) return spot;
        }
        return null;
    }
}

/////////////////////////////////////////////////////////
// Step 9 : Create PricingStrategy Class
//
// Purpose:
// PricingStrategy defines how the final parking charge is
// calculated when a vehicle exits.
//
// Pricing flow:
//     Entry Time + Exit Time
//              |
//              v
//      Parking Duration
//              |
//              v
//       Pricing Strategy
//              |
//              v
//        Final Amount
//
// Supported pricing approaches include normal, weekend and
// dynamic pricing.
//
// Benefit:
// Pricing rules can change without modifying exit processing.
//
// Design Pattern:
//     Strategy Pattern
/////////////////////////////////////////////////////////

interface PricingStrategy { double calculatePrice(Vehicle vehicle,long hours); }

class NormalPricingStrategy implements PricingStrategy
{
    public double calculatePrice(Vehicle vehicle,long hours)
    {
        if(hours<=0) hours=1;
        if(vehicle.isVip()) return hours*100;
        if(vehicle.isEv()) return hours*15;
        switch(vehicle.getVehicleType())
        {
            case BIKE: return hours*20;
            case CAR: return hours*50;
            case TRUCK: return hours*100;
            default: return 0;
        }
    }
}

class WeekendPricingStrategy implements PricingStrategy
{
    public double calculatePrice(Vehicle vehicle,long hours)
    {
        if(hours<=0) hours=1;
        double base=new NormalPricingStrategy().calculatePrice(vehicle,hours);
        return base*2;
    }
}

class DynamicPricingStrategy implements PricingStrategy
{
    public double calculatePrice(Vehicle vehicle,long hours)
    {
        if(hours<=0) hours=1;
        double base=new NormalPricingStrategy().calculatePrice(vehicle,hours);
        return base*1.25;
    }
}

/////////////////////////////////////////////////////////
// Step 10 : Create PaymentStrategy Class
//
// Purpose:
// PaymentStrategy provides a common abstraction for different
// payment methods.
//
// Payment methods:
//     1. Cash
//     2. UPI
//     3. Card
//
// Flow:
//     Final Amount
//          |
//          v
//     PaymentStrategy
//       /    |    \
//    Cash   UPI   Card
//
// ExitGate works with the common strategy and therefore does
// not require separate logic for every payment type.
//
// Design Pattern:
//     Strategy Pattern
/////////////////////////////////////////////////////////

interface PaymentStrategy { boolean pay(double amount); }
class UPIPayment implements PaymentStrategy
{
    public boolean pay(double amount){ System.out.println("UPI payment successful : Rs. "+amount); return true; }
}
class CardPayment implements PaymentStrategy
{
    public boolean pay(double amount){ System.out.println("Card payment successful : Rs. "+amount); return true; }
}
class CashPayment implements PaymentStrategy
{
    public boolean pay(double amount){ System.out.println("Cash payment successful : Rs. "+amount); return true; }
}

/////////////////////////////////////////////////////////
// Step 11 : Create ParkingTicket Class
//
// Purpose:
// ParkingTicket represents one complete parking transaction.
//
// Information maintained:
//     - Ticket number
//     - Vehicle details
//     - Floor and spot
//     - Entry time
//     - Exit time
//     - Ticket status
//     - Parking duration
//     - Final amount
//
// Ticket lifecycle:
//     Vehicle Entry -> ACTIVE Ticket -> Vehicle Exit -> CLOSED
//
// The ticket number is used to identify the active transaction
// during the exit process.
/////////////////////////////////////////////////////////

class ParkingTicket
{
    private static int counter=1000;

    /////////////////////////////////////////////////////////
    // Initialize Ticket Counter From Database
    // It avoids duplicate ticket numbers after program restart
    /////////////////////////////////////////////////////////

    public static void initializeCounter() throws SQLException
    {
        String query = "SELECT COALESCE(MAX(ticket_number),1000) FROM parking_tickets";

        try(Connection connection = DatabaseConnection.getConnection();
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery(query))
        {
            if(result.next())
            {
                counter = result.getInt(1);
            }
        }
    }

    private int ticketNumber;
    private Vehicle vehicle;
    private ParkingFloor floor;
    private ParkingSpot spot;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private TicketStatus status;
    private double finalAmount;
    private int gateNumber;

    public ParkingTicket(Vehicle vehicle,ParkingFloor floor,ParkingSpot spot,int gateNumber)
    {
        this.ticketNumber=++counter; this.vehicle=vehicle; this.floor=floor; this.spot=spot;
        this.entryTime=LocalDateTime.now(); this.status=TicketStatus.ACTIVE; this.finalAmount=0; this.gateNumber=gateNumber;
    }

    public int getTicketNumber(){ return ticketNumber; }
    public Vehicle getVehicle(){ return vehicle; }
    public ParkingFloor getFloor(){ return floor; }
    public ParkingSpot getSpot(){ return spot; }
    public LocalDateTime getEntryTime(){ return entryTime; }
    public LocalDateTime getExitTime(){ return exitTime; }
    public TicketStatus getStatus(){ return status; }
    public double getFinalAmount(){ return finalAmount; }
    public void setFinalAmount(double amount){ finalAmount=amount; }
    public void closeTicket(){ exitTime=LocalDateTime.now(); status=TicketStatus.CLOSED; }
    public long calculateHours()
    {
        LocalDateTime end=(exitTime==null)?LocalDateTime.now():exitTime;
        long minutes=Duration.between(entryTime,end).toMinutes();
        long hours=minutes/60; if(minutes%60!=0) hours++; if(hours==0) hours=1; return hours;
    }

    public void displayTicket()
    {
        System.out.println("\n---------------------------------");
        System.out.println("---------- Parking Ticket -------");
        System.out.println("---------------------------------");
        System.out.println("Ticket Number   : "+ticketNumber);
        System.out.println("Vehicle Number  : "+vehicle.getVehicleNumber());
        System.out.println("Vehicle Type    : "+vehicle.getVehicleType());
        System.out.println("VIP             : "+vehicle.isVip());
        System.out.println("EV              : "+vehicle.isEv());
        System.out.println("Handicapped     : "+vehicle.isHandicapped());
        System.out.println("Floor Number    : "+floor.getFloorNumber());
        System.out.println("Spot Number     : "+spot.getSpotNumber());
        System.out.println("Entry Gate      : "+gateNumber);
        System.out.println("Entry Time      : "+entryTime);
        System.out.println("Exit Time       : "+exitTime);
        System.out.println("Parking Duration: "+calculateHours()+" hour(s)");
        System.out.println("Final Amount    : Rs. "+finalAmount);
        System.out.println("Ticket Status   : "+status);
        System.out.println("---------------------------------");
    }
}

/////////////////////////////////////////////////////////
// Step 12 : Create EntryGate Class
//
// Purpose:
// EntryGate represents the entry point of the parking facility.
//
// Main responsibility:
//     Generate a ParkingTicket containing the vehicle, selected
//     floor, selected parking spot and entry time.
//
// Flow:
//     Vehicle
//        |
//        v
//     EntryGate
//        |
//        v
//     ParkingTicket
//
// EntryGate focuses on ticket generation while ParkingLot
// coordinates the complete parking workflow.
/////////////////////////////////////////////////////////

class EntryGate
{
    private int gateNumber;
    public EntryGate(int gateNumber){ this.gateNumber=gateNumber; }
    public int getGateNumber(){ return gateNumber; }
    public ParkingTicket generateTicket(Vehicle vehicle,ParkingFloor floor,ParkingSpot spot)
    {
        System.out.println("Vehicle entering from gate : "+gateNumber);
        return new ParkingTicket(vehicle,floor,spot,gateNumber);
    }
}

/////////////////////////////////////////////////////////
// Step 13 : Create ExitGate Class
//
// Purpose:
// ExitGate represents the exit point of the parking facility.
//
// Responsibilities:
//     1. Process the active parking ticket
//     2. Calculate parking duration
//     3. Calculate final parking charges
//     4. Process the selected payment method
//     5. Complete the ticket
//
// Exit flow:
//     Ticket
//       |
//       v
//     Duration Calculation
//       |
//       v
//     Pricing Strategy
//       |
//       v
//     Final Amount
//       |
//       v
//     Payment Strategy
//       |
//       v
//     Vehicle Exit
//
// Pricing and payment remain separate using strategy objects.
/////////////////////////////////////////////////////////

class ExitGate
{
    private int gateNumber;
    public ExitGate(int gateNumber){ this.gateNumber=gateNumber; }
    public int getGateNumber(){ return gateNumber; }
    public void processExit(ParkingTicket ticket,PricingStrategy pricingStrategy,PaymentStrategy paymentStrategy)
    {
        ticket.closeTicket();
        long hours=ticket.calculateHours();
        double amount=pricingStrategy.calculatePrice(ticket.getVehicle(),hours);
        System.out.println("\nVehicle exiting from gate : "+gateNumber);
        System.out.println("Parking Duration : "+hours);
        System.out.println("Parking charges : Rs. "+amount);
        ticket.setFinalAmount(amount);
        if(!paymentStrategy.pay(amount)) throw new RuntimeException("Payment failed");
        System.out.println("Payment completed successfully");
        ticket.displayTicket();
    }
}

/////////////////////////////////////////////////////////
// Step 14 : Create ParkingLot Class
//
// Purpose:
// ParkingLot is the central business class which coordinates
// floors, strategies, tickets, vehicles and gates.
//
// Responsibilities:
//     - Maintain parking floors
//     - Select spots using ParkingStrategy
//     - Park vehicles
//     - Generate and maintain active tickets
//     - Search vehicles
//     - Process vehicle exit
//     - Release occupied spots
//     - Synchronize data with MySQL
//
// Singleton Pattern:
// Only one ParkingLot object should control the complete
// parking facility. getInstance() returns the same object.
//
// High-level flow:
//     Vehicle
//       |
//       v
//     ParkingLot
//       |--> ParkingStrategy
//       |--> ParkingFloor
//       |--> EntryGate
//       |--> ParkingTicket
//       `--> MySQL
//
// Design Pattern:
//     Singleton Pattern
/////////////////////////////////////////////////////////

class ParkingLot
{
    private static ParkingLot instance;
    private String parkingLotName;
    private List<ParkingFloor> floors;
    private Map<Integer,ParkingTicket> activeTickets;
    private Map<String,ParkingTicket> vehicleTicketMap;
    private ParkingStrategy parkingStrategy;
    private PricingStrategy pricingStrategy;

    private ParkingLot()
    {
        floors=new ArrayList<>(); activeTickets=new HashMap<>(); vehicleTicketMap=new HashMap<>();
        parkingStrategy=new FirstAvailableParkingStrategy(); pricingStrategy=new NormalPricingStrategy();
    }

    public static synchronized ParkingLot getInstance()
    {
        if(instance==null) instance=new ParkingLot(); return instance;
    }
    public void setParkingLotName(String name){ parkingLotName=name; }
    public void addFloor(ParkingFloor floor){ floors.add(floor); }
    public List<ParkingFloor> getFloors(){ return floors; }
    public void setParkingStrategy(ParkingStrategy strategy){ parkingStrategy=strategy; }
    public void setPricingStrategy(PricingStrategy strategy){ pricingStrategy=strategy; }

    // Complete vehicle-entry workflow: validate, select spot, occupy, ticket and persist
    public ParkingTicket parkVehicle(Vehicle vehicle,EntryGate entryGate) throws SQLException
    {
        if(vehicleTicketMap.containsKey(vehicle.getVehicleNumber())) throw new RuntimeException("This vehicle is already parked");
        if(ParkingDatabase.hasMonthlyPass(vehicle.getVehicleNumber())) System.out.println("Monthly Pass : ACTIVE");

        ParkingSpot spot=parkingStrategy.findSpot(floors,vehicle);
        if(spot==null) throw new RuntimeException("Parking is full");
        ParkingFloor selectedFloor=null;
        for(ParkingFloor floor:floors)
            if(floor.getParkingSpots().contains(spot)){ selectedFloor=floor; break; }
        if(selectedFloor==null) throw new RuntimeException("Unable to identify floor");

        selectedFloor.occupySpot(spot,vehicle);
        ParkingTicket ticket=entryGate.generateTicket(vehicle,selectedFloor,spot);
        activeTickets.put(ticket.getTicketNumber(),ticket); vehicleTicketMap.put(vehicle.getVehicleNumber(),ticket);
        ParkingDatabase.saveVehicle(vehicle); ParkingDatabase.saveTicket(ticket,entryGate.getGateNumber()); ParkingDatabase.updateSpotStatus(selectedFloor,spot);
        ParkingDatabase.audit("VEHICLE_PARKED",vehicle.getVehicleNumber(),"Ticket="+ticket.getTicketNumber());
        return ticket;
    }

    // Complete vehicle-exit workflow: billing, payment, release spot and cleanup
    public void removeVehicle(int ticketNumber,ExitGate exitGate,PaymentStrategy paymentStrategy) throws SQLException
    {
        ParkingTicket ticket=activeTickets.get(ticketNumber);
        if(ticket==null) throw new RuntimeException("There is no such active ticket");
        exitGate.processExit(ticket,pricingStrategy,paymentStrategy);
        ParkingDatabase.updateTicketOnExit(ticket);
        ParkingDatabase.savePayment(ticketNumber,ParkingDatabase.getPaymentType(paymentStrategy),ticket.getFinalAmount());
        ticket.getFloor().releaseSpot(ticket.getSpot());
        ParkingDatabase.updateSpotStatus(ticket.getFloor(),ticket.getSpot());
        activeTickets.remove(ticketNumber); vehicleTicketMap.remove(ticket.getVehicle().getVehicleNumber());
        ParkingDatabase.audit("VEHICLE_EXITED",ticket.getVehicle().getVehicleNumber(),"Ticket="+ticketNumber);
        System.out.println("Vehicle removed successfully");
    }

    public void settleLostTicket(int ticketNumber,PaymentStrategy paymentStrategy) throws SQLException
    {
        ParkingTicket ticket=activeTickets.get(ticketNumber);
        if(ticket==null) throw new RuntimeException("There is no such active ticket");

        double penalty=500.0;
        if(!paymentStrategy.pay(penalty)) throw new RuntimeException("Payment failed");

        ticket.setFinalAmount(penalty);
        ticket.closeTicket();

        ParkingDatabase.updateTicketOnExit(ticket);
        ParkingDatabase.savePayment(ticketNumber,ParkingDatabase.getPaymentType(paymentStrategy),penalty);
        ticket.getFloor().releaseSpot(ticket.getSpot());
        ParkingDatabase.updateSpotStatus(ticket.getFloor(),ticket.getSpot());
        activeTickets.remove(ticketNumber);
        vehicleTicketMap.remove(ticket.getVehicle().getVehicleNumber());
        ParkingDatabase.audit("LOST_TICKET_SETTLED",ticket.getVehicle().getVehicleNumber(),"Ticket="+ticketNumber+" | Penalty=500.0");

        System.out.println("Lost ticket settled successfully");
        System.out.println("Penalty paid : Rs. "+penalty);
    }

    // Searches the currently parked vehicles using the vehicle number
    public ParkingTicket searchVehicle(String vehicleNumber){ return vehicleTicketMap.get(vehicleNumber); }
    public ParkingTicket getTicket(int ticketNumber){ return activeTickets.get(ticketNumber); }
    public void displayParkingLot()
    {
        System.out.println("\n-------------------------------");
        System.out.println("----- Parking Lot Details -----");
        System.out.println("-------------------------------");
        for(ParkingFloor floor:floors) floor.displayFloor();
    }
}

/////////////////////////////////////////////////////////
// Step 15 : Controller of the Project
//
// Purpose:
// The main class controls the console application and connects
// all previously created components.
//
// Initialization sequence:
//     1. Initialize MySQL database
//     2. Create the Singleton ParkingLot object
//     3. Create parking floors
//     4. Create parking spots
//     5. Create display boards
//     6. Register observers
//     7. Add floors to ParkingLot
//     8. Create EntryGate and ExitGate
//     9. Display the application menu
//    10. Accept user choices and execute operations
//
// The controller mainly coordinates the application. Actual
// business responsibilities remain inside their respective
// classes.
//
// This structure makes the project easier to understand,
// maintain and explain during an interview.
/////////////////////////////////////////////////////////

class ParkEase
{
    // Application entry point and console menu controller
    public static void main(String A[]) throws Exception
    {
        Scanner sobj=new Scanner(System.in);

        /////////////////////////////////////////////////////////
        // 0 : Initialize MySQL Database
        /////////////////////////////////////////////////////////
        try
        {
            DatabaseConnection.initializeDatabase();
            DatabaseConnection.ensureCompatibility();
            ParkingTicket.initializeCounter();
            System.out.println("MySQL Database connected successfully");
        }
        catch(SQLException eobj)
        {
            System.out.println("Unable to connect with MySQL Database");
            System.out.println("Database Error : "+eobj.getMessage());
            return;
        }

        ParkingLot parkingLot=ParkingLot.getInstance();
        parkingLot.setParkingLotName("ParkEase");

        /////////////////////////////////////////////////////////
        // 1 : Create Multiple Floors
        /////////////////////////////////////////////////////////
        ParkingFloor floor1=new ParkingFloor(1);
        ParkingFloor floor2=new ParkingFloor(2);

        /////////////////////////////////////////////////////////
        // 2 : Create Parking Spots
        /////////////////////////////////////////////////////////
        floor1.addParkingSpot(new BikeSpot(101));
        floor1.addParkingSpot(new BikeSpot(102));
        floor1.addParkingSpot(new CarSpot(103));
        floor1.addParkingSpot(new CarSpot(104));
        floor1.addParkingSpot(new TruckSpot(105));
        floor1.addParkingSpot(new TruckSpot(106));

        floor2.addParkingSpot(new BikeSpot(201));
        floor2.addParkingSpot(new BikeSpot(202));
        floor2.addParkingSpot(new CarSpot(203));
        floor2.addParkingSpot(new CarSpot(204));
        floor2.addParkingSpot(new TruckSpot(205));
        floor2.addParkingSpot(new TruckSpot(206));

        /////////////////////////////////////////////////////////
        // 3 : Configure Special Parking Features
        /////////////////////////////////////////////////////////
        floor1.getParkingSpots().get(2).setVip(true);
        floor1.getParkingSpots().get(3).setEv(true);
        floor2.getParkingSpots().get(2).setVip(true);
        floor2.getParkingSpots().get(3).setHandicapped(true);

        /////////////////////////////////////////////////////////
        // 4 : Create Display Boards / Observers
        /////////////////////////////////////////////////////////
        ParkingDisplayBoard board1=new ParkingDisplayBoard(floor1);
        ParkingDisplayBoard board2=new ParkingDisplayBoard(floor2);
        floor1.addObserver(board1); floor2.addObserver(board2);

        /////////////////////////////////////////////////////////
        // 5 : Add Floors To Parking Lot
        /////////////////////////////////////////////////////////
        parkingLot.addFloor(floor1); parkingLot.addFloor(floor2);

        /////////////////////////////////////////////////////////
        // 6 : Save Parking Setup Into Database
        /////////////////////////////////////////////////////////
        try
        {
            for(ParkingFloor floor:parkingLot.getFloors())
            {
                ParkingDatabase.saveFloor(floor);
                for(ParkingSpot spot:floor.getParkingSpots()) ParkingDatabase.saveSpot(floor,spot);
            }
        }
        catch(SQLException eobj){ System.out.println("Database error while saving parking setup : "+eobj.getMessage()); }

        /////////////////////////////////////////////////////////
        // 7 : Create Multiple Entry / Exit Gates
        /////////////////////////////////////////////////////////
        EntryGate entryGate1=new EntryGate(1);
        EntryGate entryGate2=new EntryGate(2);
        ExitGate exitGate1=new ExitGate(1);
        ExitGate exitGate2=new ExitGate(2);

        int choice=0;
        while(true)
        {
            System.out.println("\n---------------------------------");
            System.out.println("---- ParkEase ------------------");
            System.out.println("---------------------------------");
            System.out.println("1 : Park Vehicle");
            System.out.println("2 : Exit Vehicle");
            System.out.println("3 : Search Vehicle");
            System.out.println("4 : Display Parking Lot");
            System.out.println("5 : Parking Availability");
            System.out.println("6 : Parking History");
            System.out.println("7 : Revenue Report");
            System.out.println("8 : Parking Statistics");
            System.out.println("9 : Payment History");
            System.out.println("10 : Make Reservation");
            System.out.println("11 : Display Reservations");
            System.out.println("12 : Monthly Pass");
            System.out.println("13 : Change Parking Strategy");
            System.out.println("14 : Change Pricing Strategy");
            System.out.println("15 : Lost Ticket");
            System.out.println("16 : Admin Login / Audit Log");
            System.out.println("17 : Exit");
            System.out.println("Enter your choice : ");

            try { choice=sobj.nextInt(); }
            catch(Exception eobj){ sobj.nextLine(); System.out.println("Invalid input"); continue; }

            try
            {
                switch(choice)
                {
                    case 1:
                    {
                        System.out.println("\nSelect vehicle type :");
                        System.out.println("1 : Bike"); System.out.println("2 : Car"); System.out.println("3 : Truck");
                        int type=sobj.nextInt();
                        System.out.println("Enter vehicle number : "); String number=sobj.next();
                        VehicleType vehicleType;
                        if(type==1) vehicleType=VehicleType.BIKE;
                        else if(type==2) vehicleType=VehicleType.CAR;
                        else if(type==3) vehicleType=VehicleType.TRUCK;
                        else { System.out.println("Invalid vehicle type"); break; }

                        Vehicle vehicle=VehicleFactory.createVehicle(vehicleType,number);
                        System.out.println("VIP Vehicle? (1=Yes, 0=No) : "); vehicle.setVip(sobj.nextInt()==1);
                        System.out.println("EV Vehicle? (1=Yes, 0=No) : "); vehicle.setEv(sobj.nextInt()==1);
                        System.out.println("Handicapped Requirement? (1=Yes, 0=No) : "); vehicle.setHandicapped(sobj.nextInt()==1);
                        System.out.println("Select Entry Gate (1/2) : "); int gate=sobj.nextInt();
                        EntryGate selectedGate=(gate==2)?entryGate2:entryGate1;
                        ParkingTicket ticket=parkingLot.parkVehicle(vehicle,selectedGate);
                        ticket.displayTicket();
                        break;
                    }

                    case 2:
                    {
                        System.out.println("Enter ticket number : "); int ticketNumber=sobj.nextInt();
                        System.out.println("Enter payment option : 1 : Cash  2 : UPI  3 : Card"); int paymentType=sobj.nextInt();
                        PaymentStrategy paymentStrategy;
                        if(paymentType==1) paymentStrategy=new CashPayment();
                        else if(paymentType==2) paymentStrategy=new UPIPayment();
                        else if(paymentType==3) paymentStrategy=new CardPayment();
                        else { System.out.println("Invalid payment option"); break; }
                        System.out.println("Select Exit Gate (1/2) : "); int gate=sobj.nextInt();
                        ExitGate selectedGate=(gate==2)?exitGate2:exitGate1;
                        parkingLot.removeVehicle(ticketNumber,selectedGate,paymentStrategy);
                        break;
                    }

                    case 3:
                    {
                        System.out.println("Enter vehicle number : "); String number=sobj.next();
                        ParkingTicket ticket=parkingLot.searchVehicle(number);
                        if(ticket!=null) ticket.displayTicket();
                        else ParkingDatabase.searchVehicle(number);
                        break;
                    }

                    case 4: parkingLot.displayParkingLot(); break;
                    case 5: ParkingDatabase.showAvailability(); break;

                    case 6:
                    {
                        System.out.println("Enter vehicle number : "); ParkingDatabase.showHistory(sobj.next()); break;
                    }

                    case 7: ParkingDatabase.showRevenue(); break;
                    case 8: ParkingDatabase.showStatistics(); break;
                    case 9: ParkingDatabase.showPaymentHistory(); break;

                    case 10:
                    {
                        System.out.println("Enter vehicle number : "); String number=sobj.next();
                        System.out.println("Vehicle type (1=Bike,2=Car,3=Truck) : "); int type=sobj.nextInt();
                        VehicleType vt=(type==1)?VehicleType.BIKE:(type==2)?VehicleType.CAR:VehicleType.TRUCK;
                        ParkingDatabase.saveReservation(number,vt); ParkingDatabase.audit("RESERVATION_CREATED",number,"Reservation created");
                        System.out.println("Reservation created successfully"); break;
                    }

                    case 11: ParkingDatabase.showReservations(); break;

                    case 12:
                    {
                        System.out.println("Enter vehicle number : "); String number=sobj.next();
                        ParkingDatabase.saveMonthlyPass(number); ParkingDatabase.audit("MONTHLY_PASS_CREATED",number,"30 day pass created");
                        System.out.println("Monthly pass activated for 30 days"); break;
                    }

                    case 13:
                    {
                        System.out.println("1 : First Available"); System.out.println("2 : Nearest Available"); int option=sobj.nextInt();
                        if(option==1) parkingLot.setParkingStrategy(new FirstAvailableParkingStrategy());
                        else if(option==2) parkingLot.setParkingStrategy(new NearestAvailableParkingStrategy());
                        else { System.out.println("Invalid strategy"); break; }
                        System.out.println("Parking strategy changed successfully"); break;
                    }

                    case 14:
                    {
                        System.out.println("1 : Normal"); System.out.println("2 : Weekend"); System.out.println("3 : Dynamic"); int option=sobj.nextInt();
                        if(option==1) parkingLot.setPricingStrategy(new NormalPricingStrategy());
                        else if(option==2) parkingLot.setPricingStrategy(new WeekendPricingStrategy());
                        else if(option==3) parkingLot.setPricingStrategy(new DynamicPricingStrategy());
                        else { System.out.println("Invalid pricing strategy"); break; }
                        System.out.println("Pricing strategy changed successfully"); break;
                    }

                    case 15:
                    {
                        System.out.println("Enter lost ticket number : "); int ticketNumber=sobj.nextInt();
                        ParkingTicket ticket=parkingLot.getTicket(ticketNumber);
                        if(ticket==null)
                        {
                            System.out.println("Lost ticket not found in active tickets");
                        }
                        else
                        {
                            System.out.println("Lost Ticket Penalty : Rs. 500.0");
                            System.out.println("Payment option : 1 : Cash  2 : UPI  3 : Card");
                            int paymentType=sobj.nextInt();
                            PaymentStrategy paymentStrategy;
                            if(paymentType==1) paymentStrategy=new CashPayment();
                            else if(paymentType==2) paymentStrategy=new UPIPayment();
                            else if(paymentType==3) paymentStrategy=new CardPayment();
                            else { System.out.println("Invalid payment option"); break; }
                            parkingLot.settleLostTicket(ticketNumber,paymentStrategy);
                        }
                        break;
                    }

                    case 16:
                    {
                        System.out.println("Enter admin username : "); String user=sobj.next();
                        System.out.println("Enter admin password : "); String pass=sobj.next();
                        if(ParkingDatabase.validateAdmin(user,pass))
                        {
                            System.out.println("Admin login successful");
                            ParkingDatabase.showAuditLogs();
                        }
                        else System.out.println("Invalid admin credentials");
                        break;
                    }

                    case 17:
                        System.out.println("Thank you for using ParkEase");
                        sobj.close(); return;

                    default: System.out.println("Invalid option");
                }
            }
            catch(Exception eobj)
            {
                System.out.println("Exception occured : "+eobj.getMessage());
            }
        }
    }
}
