-- =====================================================================
-- Warehouse Management – master data tables (issue #3)
-- Target: MySQL 8.0+ (also runs on MariaDB 10.6+)
--
-- Column names and column ORDER follow the team's data sheet, so the
-- sheet's INSERT statements (which list no column names) load unchanged.
-- Transaction tables (receipts, issues, movements, counts, stock history,
-- staff, tasks) are added by the database team below these.
--
-- Naming: table and column names in lower case. MySQL on Linux treats
-- table names as case-sensitive (WAREHOUSE <> warehouse), so the whole
-- team should write them in lower case.
--
-- Re-runnable: drops and recreates the database every time.
-- =====================================================================

DROP DATABASE IF EXISTS warehouse_db;
CREATE DATABASE warehouse_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE warehouse_db;

-- ---------------------------------------------------------------------
-- Location hierarchy: warehouse > zone > aisle > bin
-- Each level is identified only inside its parent, so keys are composite.
-- IDs are codes such as W01, Z01, A01, B001 (max 40 chars).
-- ---------------------------------------------------------------------

CREATE TABLE warehouse (
    warehouse_id       VARCHAR(40)  NOT NULL,
    warehouse_address  VARCHAR(250) NULL,
    warehouse_name     VARCHAR(120) NOT NULL,
    CONSTRAINT pk_warehouse        PRIMARY KEY (warehouse_id),
    CONSTRAINT uq_warehouse_name   UNIQUE (warehouse_name),
    CONSTRAINT ck_warehouse_id     CHECK (TRIM(warehouse_id) <> ''),
    CONSTRAINT ck_warehouse_name   CHECK (TRIM(warehouse_name) <> '')
) ENGINE = InnoDB;

CREATE TABLE zone (
    zone_id            VARCHAR(40)  NOT NULL,
    warehouse_id       VARCHAR(40)  NOT NULL,
    zone_name          VARCHAR(120) NOT NULL,
    CONSTRAINT pk_zone             PRIMARY KEY (warehouse_id, zone_id),
    CONSTRAINT uq_zone_name        UNIQUE (warehouse_id, zone_name),
    CONSTRAINT ck_zone_id          CHECK (TRIM(zone_id) <> ''),
    CONSTRAINT ck_zone_name        CHECK (TRIM(zone_name) <> ''),
    CONSTRAINT fk_zone_warehouse   FOREIGN KEY (warehouse_id)
        REFERENCES warehouse (warehouse_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE aisle (
    aisle_id           VARCHAR(40)  NOT NULL,
    aisle_name         VARCHAR(120) NOT NULL,
    zone_id            VARCHAR(40)  NOT NULL,
    warehouse_id       VARCHAR(40)  NOT NULL,
    CONSTRAINT pk_aisle            PRIMARY KEY (warehouse_id, zone_id, aisle_id),
    CONSTRAINT uq_aisle_name       UNIQUE (warehouse_id, zone_id, aisle_name),
    CONSTRAINT ck_aisle_id         CHECK (TRIM(aisle_id) <> ''),
    CONSTRAINT ck_aisle_name       CHECK (TRIM(aisle_name) <> ''),
    CONSTRAINT fk_aisle_zone       FOREIGN KEY (warehouse_id, zone_id)
        REFERENCES zone (warehouse_id, zone_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE bin (
    position           VARCHAR(40)  NOT NULL,
    aisle_id           VARCHAR(40)  NOT NULL,
    bin_name           VARCHAR(120) NULL,
    zone_id            VARCHAR(40)  NOT NULL,
    warehouse_id       VARCHAR(40)  NOT NULL,
    CONSTRAINT pk_bin              PRIMARY KEY (warehouse_id, zone_id, aisle_id, position),
    CONSTRAINT ck_bin_position     CHECK (TRIM(position) <> ''),
    CONSTRAINT fk_bin_aisle        FOREIGN KEY (warehouse_id, zone_id, aisle_id)
        REFERENCES aisle (warehouse_id, zone_id, aisle_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Products
-- ---------------------------------------------------------------------

CREATE TABLE product (
    product_id          VARCHAR(40)    NOT NULL,
    unit                VARCHAR(20)    NOT NULL,
    storage_requirement VARCHAR(120)   NOT NULL,
    reorder_level       DECIMAL(12, 3) NOT NULL DEFAULT 0,
    product_name        VARCHAR(120)   NOT NULL,
    CONSTRAINT pk_product              PRIMARY KEY (product_id),
    CONSTRAINT ck_product_id           CHECK (TRIM(product_id) <> ''),
    CONSTRAINT ck_product_name         CHECK (TRIM(product_name) <> ''),
    CONSTRAINT ck_product_unit         CHECK (TRIM(unit) <> ''),
    CONSTRAINT ck_product_reorder      CHECK (reorder_level >= 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Business partners (IDs such as S01, C01)
-- ---------------------------------------------------------------------

CREATE TABLE supplier (
    supplier_id        VARCHAR(40)  NOT NULL,
    supplier_address   VARCHAR(250) NULL,
    supplier_name      VARCHAR(120) NOT NULL,
    supplier_phone     VARCHAR(20)  NULL,
    CONSTRAINT pk_supplier         PRIMARY KEY (supplier_id),
    CONSTRAINT ck_supplier_id      CHECK (TRIM(supplier_id) <> ''),
    CONSTRAINT ck_supplier_name    CHECK (TRIM(supplier_name) <> ''),
    CONSTRAINT ck_supplier_phone   CHECK (supplier_phone IS NULL OR supplier_phone REGEXP '^[0-9+() -]{6,20}$')
) ENGINE = InnoDB;

CREATE TABLE customer (
    customer_id        VARCHAR(40)  NOT NULL,
    customer_name      VARCHAR(120) NOT NULL,
    customer_address   VARCHAR(250) NULL,
    customer_phone     VARCHAR(20)  NULL,
    CONSTRAINT pk_customer         PRIMARY KEY (customer_id),
    CONSTRAINT ck_customer_id      CHECK (TRIM(customer_id) <> ''),
    CONSTRAINT ck_customer_name    CHECK (TRIM(customer_name) <> ''),
    CONSTRAINT ck_customer_phone   CHECK (customer_phone IS NULL OR customer_phone REGEXP '^[0-9+() -]{6,20}$')
) ENGINE = InnoDB;
