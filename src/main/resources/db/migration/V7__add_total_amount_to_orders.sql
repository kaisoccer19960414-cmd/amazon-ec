-- 注文の税込み合計金額を記録する。
-- 既存の注文には遡って正しい金額を入れられないため、暫定的に0を入れておく。
ALTER TABLE orders ADD COLUMN total_amount INTEGER NOT NULL DEFAULT 0;
