//package reai.timetracker.config;
//
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//
//import java.util.Collection;
//import java.util.List;
//
//public class UserPrincipal implements UserDetails {
//    private Long userId;
//    private String username;
//    private Long tenantId;
//    private Collection<? extends GrantedAuthority> authorities;
//
//    public UserPrincipal(Long userId, String username, Long tenantId) {
//        this.userId = userId;
//        this.username = username;
//        this.tenantId = tenantId;
//        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
//    }
//
//    // UserDetails implementation
//    @Override
//    public Collection<? extends GrantedAuthority> getAuthorities() {
//        return authorities;
//    }
//
//    @Override
//    public String getPassword() { return null; }
//
//    @Override
//    public String getUsername() { return username; }
//
//    @Override
//    public boolean isAccountNonExpired() { return true; }
//
//    @Override
//    public boolean isAccountNonLocked() { return true; }
//
//    @Override
//    public boolean isCredentialsNonExpired() { return true; }
//
//    @Override
//    public boolean isEnabled() { return true; }
//
//    // Getters
//    public Long getUserId() { return userId; }
//    public Long getTenantId() { return tenantId; }
//}