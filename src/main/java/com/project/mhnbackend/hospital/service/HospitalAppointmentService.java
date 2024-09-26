package com.project.mhnbackend.hospital.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.mhnbackend.hospital.domain.Hospital;
import com.project.mhnbackend.hospital.domain.HospitalAppointment;
import com.project.mhnbackend.hospital.dto.request.HospitalAppointmentRequestDTO;
import com.project.mhnbackend.hospital.dto.response.HospitalAppointmentResponseDTO;
import com.project.mhnbackend.hospital.dto.response.HospitalAppointmentStatusPutResponseDTO;
import com.project.mhnbackend.hospital.repository.HospitalAppointmentRepository;
import com.project.mhnbackend.hospital.repository.HospitalRepository;
import com.project.mhnbackend.member.domain.Member;
import com.project.mhnbackend.member.repository.MemberRepository;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class HospitalAppointmentService {
		private final HospitalAppointmentRepository hospitalAppointmentRepository;
		private final MemberRepository memberRepository;
		private final HospitalRepository hospitalRepository;
	
	// 병원 진료예약 포스트
	public HospitalAppointment createAppointment(HospitalAppointmentRequestDTO hospitalAppointmentRequestDTO) {
		Hospital hospital = hospitalRepository.findById(hospitalAppointmentRequestDTO.getHospitalId())
				.orElseThrow(() -> new RuntimeException("Hospital not found"));
		Member member = memberRepository.findById(hospitalAppointmentRequestDTO.getMemberId())
				.orElseThrow(() -> new RuntimeException("Member not found"));
		
//		// 중복 예약 체크
//		boolean isTimeSlotAvailable = checkTimeSlotAvailability(
//				hospital.getId(),
//				hospitalAppointmentRequestDTO.getAppointmentDateTime()
//		);
//
//		if (!isTimeSlotAvailable) {
//			throw new RuntimeException("이미 예약된 시간입니다.");
//		}
		
		// 중복 예약 체크 (APPROVED 상태만)
		Optional<HospitalAppointment> existingAppointment = hospitalAppointmentRepository
				.findByHospitalIdAndAppointmentDateTimeAndStatus(
						hospital.getId(),
						hospitalAppointmentRequestDTO.getAppointmentDateTime(),
						HospitalAppointment.AppointmentStatus.APPROVED
				);
		
		if (existingAppointment.isPresent()) {
			throw new RuntimeException("이미 예약된 시간입니다.");
		}
		
		HospitalAppointment hospitalAppointment = HospitalAppointment.builder()
				.appointmentDateTime(hospitalAppointmentRequestDTO.getAppointmentDateTime())
				.member(member)
				.hospital(hospital)
				.createdAt(LocalDateTime.now())
				.build();
		
		return hospitalAppointmentRepository.save(hospitalAppointment);
	}
	
//	// 진료예약 post에서 중복 예약 체크 메서드
//	private boolean checkTimeSlotAvailability(Long hospitalId, LocalDateTime dateTime) {
//		return hospitalAppointmentRepository.findByHospitalIdAndAppointmentDateTime(hospitalId, dateTime).isEmpty();
//	}
	
	
	// 수의사 페이지 - 진료예약 리스트 get
	public List<HospitalAppointmentResponseDTO> getAppointments (Long hospitalId) {
//		// 1. 병원 ID로 예약 목록 조회
//		List<HospitalAppointment> appointments = hospitalAppointmentRepository.findByHospitalId(hospitalId);
		
		// 현재 날짜와 한 달 전 날짜 계산
		LocalDateTime endDate = LocalDateTime.now();
		LocalDateTime startDate = endDate.minusMonths(1);
		
		// 수정된 레파지토리 메소드 사용
		List<HospitalAppointment> appointments = hospitalAppointmentRepository.findByHospitalIdAndCreatedAtBetween(hospitalId, startDate, endDate);
		
		// 2. 결과를 저장할 리스트 생성
		List<HospitalAppointmentResponseDTO> result = new ArrayList<> ();
		
		// 3. 각 예약에 대해 DTO 생성 및 추가
		for (HospitalAppointment appointment : appointments) {
			HospitalAppointmentResponseDTO dto = new HospitalAppointmentResponseDTO(
					appointment.getId (),
					appointment.getAppointmentDateTime(),
					appointment.getMember(),
					appointment.getHospital(),
					appointment.getCreatedAt (),
					appointment.getStatus()
			);
			result.add(dto);
		}
		
		// 4. 결과 반환
		return result;
	}
	

//
//	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
//	private Hospital hospital;
//
//	private LocalDateTime updatedAt;
//
//	@Enumerated(EnumType.STRING)
//	private HospitalAppointment.AppointmentStatus status;
	
	
	// 대기요청 수락 로직 (기존꺼)
//	public HospitalAppointmentStatusPutResponseDTO patchAppointmentStatus (Long id, HospitalAppointment.AppointmentStatus status) {
//		Optional<HospitalAppointment> appointment = hospitalAppointmentRepository.findById(id);
//		appointment.get().setStatus (status);
//		hospitalAppointmentRepository.save(appointment.get ());
//		return HospitalAppointmentStatusPutResponseDTO.builder()
//				.id(id)
//				.appointmentDateTime (appointment.get ().getAppointmentDateTime ())
//				.member(appointment.get ().getMember ())
//				.hospital (appointment.get ().getHospital ())
//				.updatedAt (LocalDateTime.now ())
//				.appointStatus(status)
//				.build();
//	}
	
	// 대기요청 수락 로직(수정버전)
	public HospitalAppointmentStatusPutResponseDTO patchAppointmentStatus(Long id, HospitalAppointment.AppointmentStatus newStatus) {
		Optional<HospitalAppointment> appointmentOpt = hospitalAppointmentRepository.findById(id);
		
		if (appointmentOpt.isEmpty()) {
			throw new RuntimeException("Appointment not found");
		}
		
		HospitalAppointment appointment = appointmentOpt.get();
		
		if (newStatus == HospitalAppointment.AppointmentStatus.APPROVED) {
			// 같은 병원의 같은 시간에 이미 APPROVED 상태인 예약이 있는지 확인
			Optional<HospitalAppointment> existingApprovedAppointment = hospitalAppointmentRepository
					.findByHospitalIdAndAppointmentDateTimeAndStatus(
							appointment.getHospital().getId(),
							appointment.getAppointmentDateTime(),
							HospitalAppointment.AppointmentStatus.APPROVED
					);
			
			if (existingApprovedAppointment.isPresent()) {
				throw new RuntimeException("이미 같은 시간에 승인된 예약이 있습니다.");
			}
		}
		
		appointment.setStatus(newStatus);
		hospitalAppointmentRepository.save(appointment);
		
		return HospitalAppointmentStatusPutResponseDTO.builder()
				.id(id)
				.appointmentDateTime(appointment.getAppointmentDateTime())
				.member(appointment.getMember())
				.hospital(appointment.getHospital())
				.updatedAt(appointment.getUpdatedAt())
				.appointStatus(newStatus)
				.build();
	}
}
