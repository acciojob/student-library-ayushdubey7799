package com.example.library.studentlibrary.repositories;

import com.example.library.studentlibrary.models.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


public interface AuthorRepository extends JpaRepository<Author, Integer> {
}
