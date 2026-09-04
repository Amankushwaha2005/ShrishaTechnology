package com.shrishatechnology.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SchemaMigrator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigrator.class);

    private final Db db;
    private volatile boolean available;

    public SchemaMigrator(Db db) {
        this.db = db;
    }

    public boolean isAvailable() {
        return available;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Applying database schema...");
        try {
            migrate();
            available = true;
            log.info("Database schema ready");
        } catch (Exception e) {
            available = false;
            log.warn("Database schema failed — login/forms/payments may not work. ({})",
                    e.getMessage());
        }
    }

    private void migrate() {
        db.execute("""
                CREATE TABLE IF NOT EXISTS users (
                  id SERIAL PRIMARY KEY,
                  provider TEXT NOT NULL DEFAULT 'local',
                  provider_id TEXT,
                  name TEXT NOT NULL,
                  email TEXT NOT NULL,
                  password_hash TEXT,
                  picture TEXT,
                  role TEXT NOT NULL DEFAULT 'user',
                  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);
        db.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_provider_provider_id ON users(provider, provider_id)");
        db.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users(email)");

        db.execute("""
                CREATE TABLE IF NOT EXISTS contact_submissions (
                  id SERIAL PRIMARY KEY,
                  name TEXT NOT NULL,
                  email TEXT NOT NULL,
                  phone TEXT,
                  message TEXT NOT NULL,
                  status TEXT NOT NULL DEFAULT 'new',
                  created_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);
        db.execute("""
                CREATE TABLE IF NOT EXISTS work_submissions (
                  id SERIAL PRIMARY KEY,
                  full_name TEXT NOT NULL,
                  email TEXT NOT NULL,
                  phone TEXT,
                  resume TEXT,
                  skill TEXT,
                  status TEXT NOT NULL DEFAULT 'new',
                  created_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);
        db.execute("CREATE INDEX IF NOT EXISTS idx_contact_created ON contact_submissions(created_at DESC)");
        db.execute("CREATE INDEX IF NOT EXISTS idx_contact_status ON contact_submissions(status)");
        db.execute("CREATE INDEX IF NOT EXISTS idx_work_created ON work_submissions(created_at DESC)");
        db.execute("CREATE INDEX IF NOT EXISTS idx_work_status ON work_submissions(status)");

        db.execute("""
                CREATE TABLE IF NOT EXISTS chat_messages (
                  id SERIAL PRIMARY KEY,
                  message TEXT NOT NULL,
                  page_url TEXT,
                  status TEXT NOT NULL DEFAULT 'new',
                  created_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);
        db.execute("CREATE INDEX IF NOT EXISTS idx_chat_created ON chat_messages(created_at DESC)");
        db.execute("CREATE INDEX IF NOT EXISTS idx_chat_status ON chat_messages(status)");

        db.execute("""
                CREATE TABLE IF NOT EXISTS orders (
                  id SERIAL PRIMARY KEY,
                  public_id TEXT NOT NULL UNIQUE,
                  name TEXT NOT NULL,
                  email TEXT NOT NULL,
                  phone TEXT,
                  service TEXT NOT NULL,
                  plan TEXT NOT NULL,
                  notes TEXT,
                  total_inr INTEGER NOT NULL,
                  amount_inr INTEGER NOT NULL,
                  advance_percent INTEGER NOT NULL,
                  status TEXT NOT NULL DEFAULT 'pending',
                  razorpay_order_id TEXT,
                  razorpay_payment_id TEXT,
                  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                  paid_at TIMESTAMP,
                  advance_paid_inr INTEGER,
                  balance_paid_inr INTEGER NOT NULL DEFAULT 0,
                  delivered_at TIMESTAMP,
                  razorpay_balance_order_id TEXT,
                  razorpay_balance_payment_id TEXT,
                  balance_paid_at TIMESTAMP,
                  completed_at TIMESTAMP
                )
                """);
        db.execute("CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status)");
        db.execute("CREATE INDEX IF NOT EXISTS idx_orders_created ON orders(created_at DESC)");
        db.execute("CREATE INDEX IF NOT EXISTS idx_orders_razorpay ON orders(razorpay_order_id)");

        addColumnIfMissing("users", "role", "TEXT NOT NULL DEFAULT 'user'");
        addColumnIfMissing("orders", "advance_paid_inr", "INTEGER");
        addColumnIfMissing("orders", "balance_paid_inr", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("orders", "delivered_at", "TIMESTAMP");
        addColumnIfMissing("orders", "razorpay_balance_order_id", "TEXT");
        addColumnIfMissing("orders", "razorpay_balance_payment_id", "TEXT");
        addColumnIfMissing("orders", "balance_paid_at", "TIMESTAMP");
        addColumnIfMissing("orders", "completed_at", "TIMESTAMP");

        db.execute("""
                UPDATE orders
                SET status = 'advance_paid',
                    advance_paid_inr = amount_inr
                WHERE status = 'paid' AND advance_paid_inr IS NULL
                """);
        db.execute("CREATE INDEX IF NOT EXISTS idx_orders_balance_razorpay ON orders(razorpay_balance_order_id)");

        db.execute("""
                CREATE TABLE IF NOT EXISTS site_campaigns (
                  id SERIAL PRIMARY KEY,
                  kind TEXT NOT NULL,
                  title TEXT NOT NULL,
                  body TEXT,
                  cta_label TEXT,
                  cta_url TEXT,
                  media_url TEXT,
                  media_kind TEXT,
                  placement TEXT NOT NULL DEFAULT 'gallery',
                  active INTEGER NOT NULL DEFAULT 1,
                  sort_order INTEGER NOT NULL DEFAULT 0,
                  starts_at TIMESTAMP,
                  ends_at TIMESTAMP,
                  created_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);
        db.execute("CREATE INDEX IF NOT EXISTS idx_campaigns_active ON site_campaigns(active, placement)");
    }

    private void addColumnIfMissing(String table, String column, String typeSql) {
        try {
            db.execute("ALTER TABLE " + table + " ADD COLUMN IF NOT EXISTS " + column + " " + typeSql);
        } catch (Exception ignored) {
            // Existing Postgres databases may already have the column
        }
    }
}
