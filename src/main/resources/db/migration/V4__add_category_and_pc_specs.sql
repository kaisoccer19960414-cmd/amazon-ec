ALTER TABLE products ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT 'ACCESSORY';

CREATE TABLE pc_specs (
                          product_id VARCHAR(20) PRIMARY KEY REFERENCES products(id),
                          ram_gb INT NOT NULL,
                          ssd_gb INT NOT NULL,
                          cpu_maker VARCHAR(10) NOT NULL,
                          has_gpu BOOLEAN
);