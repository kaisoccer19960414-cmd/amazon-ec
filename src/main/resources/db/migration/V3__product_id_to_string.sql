DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS products;

CREATE TABLE products (
                          id VARCHAR(20) PRIMARY KEY,
                          name VARCHAR(255) NOT NULL,
                          price INT NOT NULL,
                          cached_stock INT NOT NULL,
                          updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE orders (
                        id VARCHAR(50) PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        product_id VARCHAR(20) NOT NULL REFERENCES products(id),
                        quantity INT NOT NULL,
                        reservation_id BIGINT,
                        transaction_id BIGINT,
                        status VARCHAR(30) NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT now(),
                        updated_at TIMESTAMP NOT NULL DEFAULT now()
);