package com.dea42.genspring.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.dea42.genspring.entity.Account;

/**
 * Title: UserRepository <br>
 * Description: Class for the User Repository. <br>
 * Copyright: Copyright (c) 2001-2020<br>
 * Company: RMRR<br>
 * @author Gened by com.dea42.build.GenSpring version 0.2.3<br>
 * @version 1.0.0<br>
 */
@Repository
public interface UserRepository extends JpaRepository<Account, Integer>{
	Account findOneByEmail(String email);
	Account findOneByEmailAndPassword(String email, String password);

}
