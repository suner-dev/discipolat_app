package com.discipolat.modules.members.api;

import com.discipolat.modules.members.domain.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    /** Dashboard agrégé du membre connecté (tout utilisateur authentifié). */
    @GetMapping("/me/dashboard")
    public ResponseEntity<MemberDashboardResponse> myDashboard() {
        return ResponseEntity.ok(memberService.getMyDashboard());
    }

    /** Mise à jour du profil du membre connecté (compte + âme liée). */
    @PutMapping("/me/profile")
    public ResponseEntity<MemberDashboardResponse> updateProfile(
            @Valid @RequestBody UpdateMemberProfileRequest request) {
        return ResponseEntity.ok(memberService.updateMyProfile(request));
    }
}
