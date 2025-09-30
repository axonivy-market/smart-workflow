package com.axonivy.utils.smart.workflow.demo.shopping.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.axonivy.utils.smart.workflow.demo.shopping.brand.Brand;
import com.axonivy.utils.smart.workflow.demo.shopping.brand.BrandRepository;
import com.axonivy.utils.smart.workflow.demo.shopping.product.Product;
import com.axonivy.utils.smart.workflow.demo.shopping.product.ProductRepository;
import com.axonivy.utils.smart.workflow.demo.shopping.product.image.ProductImage;
import com.axonivy.utils.smart.workflow.demo.shopping.product.image.ProductImageRepository;
import com.axonivy.utils.smart.workflow.demo.shopping.productcategory.ProductCategory;
import com.axonivy.utils.smart.workflow.demo.shopping.productcategory.ProductCategoryRepository;
import com.axonivy.utils.smart.workflow.demo.shopping.supplier.Supplier;
import com.axonivy.utils.smart.workflow.demo.shopping.supplier.SupplierRepository;
import com.axonivy.utils.smart.workflow.utils.JsonUtils;

import ch.ivyteam.ivy.environment.Ivy;

public final class DataGenerationUtils {
  
  private static final String DATA_PATH = "/com/axonivy/utils/smart/workflow/demo/shopping/utils/data/";
  
  /**
   * Creates all test data by loading from JSON files and saving to Axon Ivy Repository
   */
  public static void createData() {
    try {
      Ivy.log().info("Starting data generation...");
      
      // Load data in dependency order
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
  
  /**
   * Creates suppliers from supplier.json
   */
  private static void createSuppliers() {
    try {
      Ivy.log().info("Creating suppliers...");
      String jsonContent = loadJsonFile("supplier.json");
      List<Supplier> suppliers = JsonUtils.jsonValueToEntities(jsonContent, Supplier.class);
      
      SupplierRepository repository = SupplierRepository.getInstance();
      for (Supplier supplier : suppliers) {
        repository.create(supplier);
        Ivy.log().debug("Created supplier: " + supplier.getBusinessName() + " (ID: " + supplier.getSupplierId() + ")");
      }
      
      Ivy.log().info("Created " + suppliers.size() + " suppliers");
    } catch (Exception e) {
      Ivy.log().error("Failed to create suppliers: " + e.getMessage(), e);
      throw new RuntimeException("Failed to create suppliers", e);
    }
  }
  
  /**
   * Creates product categories from category.json
   */
  private static void createCategories() {
    try {
      Ivy.log().info("Creating product categories...");
      String jsonContent = loadJsonFile("category.json");
      List<ProductCategory> categories = JsonUtils.jsonValueToEntities(jsonContent, ProductCategory.class);
      
      ProductCategoryRepository repository = ProductCategoryRepository.getInstance();
      for (ProductCategory category : categories) {
        repository.create(category);
        Ivy.log().debug("Created category: " + category.getName() + " (ID: " + category.getCategoryId() + ")");
      }
      
      Ivy.log().info("Created " + categories.size() + " product categories");
    } catch (Exception e) {
      Ivy.log().error("Failed to create categories: " + e.getMessage(), e);
      throw new RuntimeException("Failed to create categories", e);
    }
  }
  
  /**
   * Creates brands from brand.json
   */
  private static void createBrands() {
    try {
      Ivy.log().info("Creating brands...");
      String jsonContent = loadJsonFile("brand.json");
      List<Brand> brands = JsonUtils.jsonValueToEntities(jsonContent, Brand.class);
      
      BrandRepository repository = BrandRepository.getInstance();
      for (Brand brand : brands) {
        repository.create(brand);
        Ivy.log().debug("Created brand: " + brand.getName() + " (ID: " + brand.getBrandId() + ")");
      }
      
      Ivy.log().info("Created " + brands.size() + " brands");
    } catch (Exception e) {
      Ivy.log().error("Failed to create brands: " + e.getMessage(), e);
      throw new RuntimeException("Failed to create brands", e);
    }
  }
  
  /**
   * Creates product images from product-image.json
   */
  private static void createProductImages() {
    try {
      Ivy.log().info("Creating product images...");
      String jsonContent = loadJsonFile("product-image.json");
      List<ProductImage> images = JsonUtils.jsonValueToEntities(jsonContent, ProductImage.class);
      
      ProductImageRepository repository = ProductImageRepository.getInstance();
      for (ProductImage image : images) {
        repository.create(image);
        Ivy.log().debug("Created product image with ID: " + image.getImageId());
      }
      
      Ivy.log().info("Created " + images.size() + " product images");
    } catch (Exception e) {
      Ivy.log().error("Failed to create product images: " + e.getMessage(), e);
      throw new RuntimeException("Failed to create product images", e);
    }
  }
  
  /**
   * Creates products from product.json
   */
  private static void createProducts() {
    try {
      Ivy.log().info("Creating products...");
      String jsonContent = loadJsonFile("product.json");
      List<Product> products = JsonUtils.jsonValueToEntities(jsonContent, Product.class);
      
      ProductRepository repository = ProductRepository.getInstance();
      for (Product product : products) {
        repository.createFromFile(product);
        Ivy.log().debug("Created product: " + product.getName() + " (ID: " + product.getProductId() + ")");
      }
      
      Ivy.log().info("Created " + products.size() + " products");
    } catch (Exception e) {
      Ivy.log().error("Failed to create products: " + e.getMessage(), e);
      throw new RuntimeException("Failed to create products", e);
    }
  }
  
  /**
   * Loads JSON content from a file in the data folder
   * @param filename the JSON file name (e.g., "supplier.json")
   * @return the JSON content as a string
   * @throws IOException if the file cannot be read
   */
  private static String loadJsonFile(String filename) throws IOException {
    String resourcePath = DATA_PATH + filename;
    
    try (InputStream inputStream = DataGenerationUtils.class.getResourceAsStream(resourcePath)) {
      if (inputStream == null) {
        throw new IOException("Resource not found: " + resourcePath);
      }
      
      return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }
  }
}
