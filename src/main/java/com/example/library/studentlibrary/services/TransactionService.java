package com.example.library.studentlibrary.services;

import com.example.library.studentlibrary.models.*;
import com.example.library.studentlibrary.repositories.BookRepository;
import com.example.library.studentlibrary.repositories.CardRepository;
import com.example.library.studentlibrary.repositories.TransactionRepository;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
@Builder
@Service
public class TransactionService {

    @Autowired
    BookRepository bookRepository5;

    @Autowired
    CardRepository cardRepository5;

    @Autowired
    TransactionRepository transactionRepository5;

    @Value("${books.max_allowed}")
    int max_allowed_books;

    @Value("${books.max_allowed_days}")
    int getMax_allowed_days;

    @Value("${books.fine.per_day}")
    int fine_per_day;

    public String issueBook(int cardId, int bookId) throws Exception {
        //check whether bookId and cardId already exist
        //conditions required for successful transaction of issue book:
        //1. book is present and available
        // If it fails: throw new Exception("Book is either unavailable or not present");
        //2. card is present and activated
        // If it fails: throw new Exception("Card is invalid");
        //3. number of books issued against the card is strictly less than max_allowed_books
        // If it fails: throw new Exception("Book limit has reached for this card");
        //If the transaction is successful, save the transaction to the list of transactions and return the id

        //Note that the error message should match exactly in all cases

       Book book = bookRepository5.findById(bookId).get();
       Card card = cardRepository5.findById(cardId).get();
        try{
           if(book==null || book.isAvailable()==false){
               throw new Exception("Book is either unavailable or not present");
           }
           if(card==null || card.getCardStatus()==CardStatus.DEACTIVATED){
               throw new Exception("Card is invalid");
           }
           if(card.getBooks().size()>=max_allowed_books){
               throw new Exception("Book limit has reached for this card");
           }
       }
       catch(Exception e){
           Transaction transaction = Transaction.builder()
                   .isIssueOperation(false)
                   .transactionStatus(TransactionStatus.FAILED)
                   .build();
           transactionRepository5.save(transaction);
           return transaction.getTransactionId();
       }
        Transaction transaction = Transaction.builder()
                .book(book)
                .card(card)
                .isIssueOperation(true)
                .transactionStatus(TransactionStatus.SUCCESSFUL)
                .build();
        transactionRepository5.save(transaction);
        book.setAvailable(false);
        List<Book> books = card.getBooks();
        books.add(book);
        card.setBooks(books);
        bookRepository5.save(book);
        cardRepository5.save(card);
       return transaction.getTransactionId(); //return transactionId instead
    }

    public Transaction returnBook(int cardId, int bookId) throws Exception{

        List<Transaction> transactions = transactionRepository5.find(cardId, bookId,TransactionStatus.SUCCESSFUL, true);
        Transaction transaction = transactions.get(transactions.size() - 1);

        //for the given transaction calculate the fine amount considering the book has been returned exactly when this function is called
        //make the book available for other users
        //make a new transaction for return book which contains the fine amount as well
        Transaction transaction1 = Transaction.builder().isIssueOperation(false).build();
        long differenceInMillies = Math.abs(transaction1.getTransactionDate().getTime()-transaction1.getTransactionDate().getTime());
        long difference = TimeUnit.DAYS.convert(differenceInMillies,TimeUnit.MILLISECONDS);
        if(difference>getMax_allowed_days){
            transaction1.setFineAmount(transaction1.getFineAmount()+(int)(difference-getMax_allowed_days)+fine_per_day);
        }
        Book book = bookRepository5.findById(bookId).get();
        book.setAvailable(true);
        bookRepository5.save(book);
        Card card = cardRepository5.findById(cardId).get();
        List<Book> books = card.getBooks();
        books.remove(book);
        card.setBooks(books);
        cardRepository5.save(card);
        transaction1.setTransactionStatus(TransactionStatus.SUCCESSFUL);
        transactionRepository5.save(transaction1);
        Transaction returnBookTransaction  = transaction1;
        return returnBookTransaction; //return the transaction after updating all details
    }
}