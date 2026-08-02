package com.accountingapp.user;

import com.accountingapp.user.dto.AddMemberRequest;
import com.accountingapp.user.dto.MemberDto;
import com.accountingapp.user.dto.UpdateMemberRoleRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Transactional
public class OrganizationMemberController {

    private final OrganizationMemberService organizationMemberService;
    private final OrganizationMemberMapper organizationMemberMapper;

    @GetMapping
    public List<MemberDto> list() {
        return organizationMemberService.list().stream().map(organizationMemberMapper::toDto).toList();
    }

    @PostMapping
    @PreAuthorize("@access.isOrgAdmin()")
    public ResponseEntity<MemberDto> add(@Valid @RequestBody AddMemberRequest request) {
        MemberDto dto = organizationMemberMapper.toDto(organizationMemberService.add(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("@access.isOrgAdmin()")
    public MemberDto updateRole(@PathVariable UUID id, @Valid @RequestBody UpdateMemberRoleRequest request) {
        return organizationMemberMapper.toDto(organizationMemberService.updateRole(id, request.role()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@access.isOrgAdmin()")
    public ResponseEntity<Void> remove(@PathVariable UUID id) {
        organizationMemberService.remove(id);
        return ResponseEntity.noContent().build();
    }
}
