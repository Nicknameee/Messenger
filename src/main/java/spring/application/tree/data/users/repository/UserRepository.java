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
    @Query("DELETE FROM AbstractUserModel user WHERE user.id = :id")
    void deleteAbstractUserModelById(@Param("id") Integer id);
}