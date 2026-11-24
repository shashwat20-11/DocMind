package com.DocMind.entity;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private  Long id;

	    @Column(unique = true,nullable = false)
	    private  String username;

	    @Column(unique = false)
	    private  String password;

	    @Enumerated(EnumType.STRING)
	    private  Role role;

	    @Column(unique = true,nullable = false)
	    private  String email;

//	    @OneToMany(mappedBy = "user",cascade = CascadeType.ALL,orphanRemoval = true)
//	    private Set<DocumentMetaData> documentMetadataSet=new LinkedHashSet<>();
	    
}
