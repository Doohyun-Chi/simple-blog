package com.simpleblog.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.simpleblog.entity.Book;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** The books of the shelf, newest registered first; ordered by registered date, which IDX_TB_BOOK_REGISTERED_DATE serves. */
public interface BookRepository extends Repository<Book, Long> {

	Book save(Book book);

	Optional<Book> findById(Long id);

	void delete(Book book);

	@Query("select b from Book b order by b.registeredDate desc, b.id desc")
	List<Book> listOfBooks();

	/** The books registered from {@code first} up to, not including, {@code before}. */
	@Query("select b from Book b where b.registeredDate >= :first and b.registeredDate < :before order by b.registeredDate desc, b.id desc")
	List<Book> listOfBooksRegisteredBetween(@Param("first") LocalDate first, @Param("before") LocalDate before);

	/** The years that have books, newest first. */
	@Query("select distinct year(b.registeredDate) from Book b order by 1 desc")
	List<Integer> listOfYears();

}
