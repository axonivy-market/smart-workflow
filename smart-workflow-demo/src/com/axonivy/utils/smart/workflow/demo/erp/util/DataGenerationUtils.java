package com.axonivy.utils.smart.workflow.demo.erp.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.axonivy.utils.smart.workflow.demo.erp.brand.model.Brand;
import com.axonivy.utils.smart.workflow.demo.erp.brand.repository.BrandRepository;
import com.axonivy.utils.smart.workflow.demo.erp.product.category.ProductCategory;
import com.axonivy.utils.smart.workflow.demo.erp.product.category.ProductCategoryRepository;
import com.axonivy.utils.smart.workflow.demo.erp.product.model.Product;
import com.axonivy.utils.smart.workflow.demo.erp.product.model.ProductImage;
import com.axonivy.utils.smart.workflow.demo.erp.product.repository.ProductImageRepository;
import com.axonivy.utils.smart.workflow.demo.erp.product.repository.ProductRepository;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.Supplier;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.repository.SupplierRepository;
import com.axonivy.utils.smart.workflow.utils.JsonUtils;

import ch.ivyteam.ivy.environment.Ivy;

public final class DataGenerationUtils {
  
  private static final String DATA_PATH = "/com/axonivy/utils/smart/workflow/demo/erp/util/data/";
  
  public static void createData() {
    try {
      Ivy.log().info("Starting data generation...");
      
      createSuppliers();
      createCategories();
      createBrands();
      createProductImages();
      createProducts();
      
      Ivy.log().info("Data generation completed successfully!");
      
    } catch (Exception e) {
      Ivy.log().error("Failed to create data: " + e.getMessage(), e);
      throw new RuntimeException("Data generation failed", e);
    }
  }
  
  private static void createSuppliers() {
    List<Supplier> suppliers = loadEntities("supplier.json", Supplier.class);
    SupplierRepository repository = SupplierRepository.getInstance();
    suppliers.forEach(repository::create);
    Ivy.log().info("Created " + suppliers.size() + " suppliers");
  }

  private static void createCategories() {
    List<ProductCategory> categories = loadEntities("category.json", ProductCategory.class);
    ProductCategoryRepository repository = ProductCategoryRepository.getInstance();
    categories.forEach(repository::create);
    Ivy.log().info("Created " + categories.size() + " product categories");
  }

  private static void createBrands() {
    List<Brand> brands = loadEntities("brand.json", Brand.class);
    BrandRepository repository = BrandRepository.getInstance();
    brands.forEach(repository::create);
    Ivy.log().info("Created " + brands.size() + " brands");
  }

  private static void createProductImages() {
    List<ProductImage> images = loadEntities("product-image.json", ProductImage.class);
    ProductImageRepository repository = ProductImageRepository.getInstance();
    images.forEach(repository::create);
    Ivy.log().info("Created " + images.size() + " product images");
  }

  private static void createProducts() {
    List<Product> products = loadEntities("product.json", Product.class);
    ProductRepository repository = ProductRepository.getInstance();
    products.forEach(repository::createFromFile);
    Ivy.log().info("Created " + products.size() + " products");
  }

  private static <T> List<T> loadEntities(String filename, Class<T> type) {
    String resourcePath = DATA_PATH + filename;
    try (InputStream inputStream = DataGenerationUtils.class.getResourceAsStream(resourcePath)) {
      if (inputStream == null) {
        throw new IOException("Resource not found: " + resourcePath);
      }
      String jsonContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
      return JsonUtils.jsonValueToEntities(jsonContent, type);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load " + filename, e);
    }
  }
}
