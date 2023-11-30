package oauth2.jwt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class DynamicEntityService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void createDynamicEntity(String tableName, List<Map<String, String>> records) {
        createTableIfNotExists(tableName, records.get(0));
        records.forEach(record->{
            StringBuffer insertQuery = new StringBuffer();
            insertQuery.append("insert into " + tableName + "(");
            insertQuery.append(String.join(",", record.keySet()));
            insertQuery.append(") values (");
            Collection<String> params = record.values();
            String paramQuery = "?,".repeat(params.size()-1);
            insertQuery.append(paramQuery).append("?)");
            System.out.println(insertQuery);
            jdbcTemplate.update(insertQuery.toString(), params.toArray());
        });
    }

    private void createTableIfNotExists(String tableName, Map<String, String> record) {
        StringBuffer sb = new StringBuffer();
        sb.append("create table if not exists " + tableName + " (\n" );
        sb.append(String.join(" varchar(255),\n", record.keySet()));
        sb.append(" varchar(255),\n");
        sb.append("id bigint auto_increment primary key)");
        jdbcTemplate.execute(sb.toString());
    }

    public List findAll() {
        return jdbcTemplate.queryForList("select * from products");
    }
}