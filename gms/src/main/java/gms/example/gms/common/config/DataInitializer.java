package gms.example.gms.common.config;

import gms.example.gms.staff.entity.Staff;
import gms.example.gms.staff.enums.StaffRole;
import gms.example.gms.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (staffRepository.existsByRole(StaffRole.MANAGER)) {
            return;
        }

        Staff admin = new Staff();
        admin.setUsername("chuongtdq");
        admin.setPasswordHash(passwordEncoder.encode("123456"));
        admin.setRole(StaffRole.MANAGER);
        admin.setEnabled(true);
        admin.setFullName("MANAGER");

        staffRepository.save(admin);
    }
}
