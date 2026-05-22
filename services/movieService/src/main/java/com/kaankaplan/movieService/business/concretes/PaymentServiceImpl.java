package com.kaankaplan.movieService.business.concretes;

import com.kaankaplan.movieService.business.abstracts.PaymentService;
import com.kaankaplan.movieService.entity.dto.EmailMessageKafkaDto;
import com.kaankaplan.movieService.entity.dto.TicketInformationDto;
import com.kaankaplan.movieService.kafka.KafkaProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final KafkaProducer kafkaProducer;

    @Value("${cinevision.mail.from:noreply@cinevision.local}")
    private String sender;

    @Override
    public void sendTicketDetail(TicketInformationDto ticketInformationDto) {
        String ticketReference = UUID.randomUUID().toString();

        EmailMessageKafkaDto emailMessage = EmailMessageKafkaDto.builder()
                .sender(sender)
                .recipient(ticketInformationDto.getEmail())
                .subtitle("Your CineVision ticket details")
                .fullName(ticketInformationDto.getFullName())
                .movieName(ticketInformationDto.getMovieName())
                .movieDay(ticketInformationDto.getMovieDay())
                .movieStartTime(ticketInformationDto.getMovieStartTime())
                .saloonName(ticketInformationDto.getSaloonName())
                .chairNumbers(ticketInformationDto.getChairNumbers())
                .ticketReference(ticketReference)
                .build();

        kafkaProducer.sendMessage(emailMessage);
    }
}
