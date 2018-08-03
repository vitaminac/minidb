package scheduler;

public interface Executor<DTO> {
    void execute(DTO dto);
}