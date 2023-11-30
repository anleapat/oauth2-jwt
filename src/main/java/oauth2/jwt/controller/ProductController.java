package oauth2.jwt.controller;

import oauth2.jwt.dto.ProductInfoDto;
import oauth2.jwt.service.DynamicEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {
    @Autowired
    DynamicEntityService dynamicEntityService;

    @PostMapping("/add")
    public String addProduct(@RequestBody ProductInfoDto productInfoDto) {
        dynamicEntityService.createDynamicEntity(productInfoDto.getTable(), productInfoDto.getRecords());
        return "Product Added Successfully";
    }

    @GetMapping("/all")
    public List allProduct() {
        return dynamicEntityService.findAll();
    }
}
