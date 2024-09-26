package com.project.mhnbackend.hospital.repository;

import com.project.mhnbackend.hospital.domain.HospitalAppointment;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HospitalAppointmentRepository extends JpaRepository<HospitalAppointment,Long> {
//	Optional<HospitalAppointment> findByHospitalIdAndAppointmentDateTime(Long hospitalId, LocalDateTime appointmentDateTime);
	
	Optional<HospitalAppointment> findByHospitalIdAndAppointmentDateTimeAndStatus(
			Long hospitalId,
			LocalDateTime appointmentDateTime,
			HospitalAppointment.AppointmentStatus status
	);
	
	List<HospitalAppointment> findByHospitalId(Long hospitalId);
	
	@Query("SELECT ha FROM HospitalAppointment ha WHERE ha.hospital.id = :hospitalId AND ha.createdAt BETWEEN :startDate AND :endDate")
	List<HospitalAppointment> findByHospitalIdAndCreatedAtBetween(
			@Param ("hospitalId") Long hospitalId,
			@Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate
	);
}
