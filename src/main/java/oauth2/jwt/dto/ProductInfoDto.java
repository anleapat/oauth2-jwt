package oauth2.jwt.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProductInfoDto {
    private String table;
    private List<Map<String, String>> records;
}
