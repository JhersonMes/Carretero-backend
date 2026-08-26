package com.carretero.service.implementation;

import com.carretero.model.User;
import com.carretero.repository.IGenericRepository;
import com.carretero.repository.IUserRepository;
import com.carretero.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService extends GenericService<User, Integer> implements IUserService {

    private final IUserRepository repo;
    private final PasswordEncoder passwordEncoder;

    @Override
    protected IGenericRepository<User, Integer> getRepo() {
        return repo;
    }

    @Override
    public User save(User user) throws Exception {
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return super.save(user);
    }

    @Override
    public User update(User user, Integer id) throws Exception {
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            User existingUser = repo.findById(id).orElse(null);
            if (existingUser != null) {
                user.setPassword(existingUser.getPassword());
            }
        }
        return super.update(user, id);
    }
}
