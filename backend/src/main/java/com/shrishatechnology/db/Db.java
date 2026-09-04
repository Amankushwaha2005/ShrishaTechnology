package com.shrishatechnology.db;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class Db {

    private final JdbcTemplate jdbc;

    public Db(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void execute(String sql, Object... args) {
        if (args == null || args.length == 0) {
            jdbc.execute(sql);
        } else {
            jdbc.update(sql, args);
        }
    }

    public List<Map<String, Object>> query(String sql, Object... args) {
        return jdbc.query(sql, new ColumnMapRowMapper(), args);
    }

    public Map<String, Object> queryOne(String sql, Object... args) {
        List<Map<String, Object>> rows = query(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void updateEmailsAny(String sql, List<String> emails) {
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            Array arr = con.createArrayOf("text", emails.toArray());
            ps.setArray(1, arr);
            return ps;
        });
    }

    public boolean ping() {
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (DataAccessException e) {
            return false;
        }
    }
}
