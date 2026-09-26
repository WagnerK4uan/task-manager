package com.taskmanager.infrastructure.persistence;

import com.taskmanager.domain.entity.Task;
import com.taskmanager.domain.enums.TaskStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** O adaptador de saída concreto: Spring Data sobre a entidade {@link Task}. */
public interface TaskJpaRepository extends JpaRepository<Task, Long> {

    @Query(
            """
            select t from Task t
             where (:titulo is null or lower(t.title) like lower(concat('%', :titulo, '%')))
               and (:status is null or t.status = :status)
             order by t.createdAt desc
            """)
    List<Task> buscar(@Param("titulo") String titulo, @Param("status") TaskStatus status);
}
