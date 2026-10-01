package com.dayspark.app.model;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="progress_logs")
public class ProgressLog {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="dare_id",nullable=false) private Dare dare;
    @Column(nullable=false,name="progress_date") private LocalDate day;
    @Column(nullable=false) private int amount;
    @Column(nullable=false,length=12) private String source;
    @Column(nullable=false) private LocalDateTime loggedAt;
    protected ProgressLog(){}
    public ProgressLog(Dare dare, LocalDate day,int amount,String source){this.dare=dare;this.day=day;this.amount=amount;this.source=source;this.loggedAt=LocalDateTime.now();}
    public Long getId(){return id;} public Dare getDare(){return dare;} public LocalDate getDay(){return day;}
    public int getAmount(){return amount;} public String getSource(){return source;} public LocalDateTime getLoggedAt(){return loggedAt;}
}
