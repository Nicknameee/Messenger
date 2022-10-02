package spring.application.tree.data.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import spring.application.tree.data.users.models.AbstractUserModel;

public interface UserRepository extends JpaRepository<AbstractUserModel, Integer> {
    @Query("SELECT user FROM AbstractUserModel user WHERE user.email = :login OR user.username = :login")
    AbstractUserModel findUserByLogin(@Param("login") String login);
    @Query(value = "SELECT COUNT(user) FROM AbstractUserModel user WHERE user.email = :email OR user.username = :username")
    Long countAbstractUserModelsWithFollowingEmailOrUsername(@Param("email") String email, @Param("username") String username);
    @Query(value = "SELECT COUNT(user) FROM AbstractUserModel user WHERE user.email = :email")
    Long countAbstractUserModelsWithFollowingEMail(@Param("email") String email);
    @Query(value = "SELECT COUNT(user) FROM AbstractUserModel user WHERE user.username = :username")
    Long countAbstractUserModelsWithFollowingUsername(@Param("username") String username);
    @Modifying
    @Transactional
    @Query("UPDATE AbstractUserModel u SET u.loginTime = current_timestamp WHERE u.username = :username")
    void updateUserLoginTimeByUsername(@Param("username") String username);
    @Modifying
    @Transactional
    @Query("UPDATE AbstractUserModel u SET u.logoutTime = current_timestamp WHERE u.username = :username")
    void updateUserLogoutTimeByUsername(@Param("username") String username);
    @Modifying
    @Transactional
    @Query("UPDATE AbstractUserModel u SET u.password = :password WHERE u.username = :login OR u.email = :login")
    void updateUserPassword(@Param("login") String login, @Param("password") String password);
    @Modifying
    @Transactional
    @Query("UPDATE AbstractUserModel u SET u.status = 'ENABLED' WHERE u.email = :email")
    void enableUser(@Param("email") String email);
    @Modifying
    @Transactional
    @Query("UPDATE AbstractUserModel u SET u.status = 'DISABLED' WHERE u.email = :email")
    void disableUser(@Param("email") String email);
    @Modifying
    @Transactional
    @Query("DELETE FROM AbstractUserModel user WHERE user.id = :id")
    void deleteAbstractUserModelById(@Param("id") Integer id);
    @Modifying
    @Transactional
    @Query("DELETE FROM AbstractUserModel user WHERE user.email = :login AND user.status = 'DISABLED' OR user.username = :login AND user.status = 'DISABLED'")
    void deleteDisabledUser(@Param("login") String login);
}
