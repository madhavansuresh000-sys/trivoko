package spike;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
@Indexed
public class Product {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id;

	@FullTextField
	String title;

	@KeywordField(aggregable = org.hibernate.search.engine.backend.types.Aggregable.YES)
	String brand;

	protected Product() {
	}

	Product(String title, String brand) {
		this.title = title;
		this.brand = brand;
	}

	public String getTitle() {
		return title;
	}
}
