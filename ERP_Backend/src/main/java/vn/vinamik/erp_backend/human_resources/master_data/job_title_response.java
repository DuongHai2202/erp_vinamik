package vn.vinamik.erp_backend.human_resources.master_data;

public record job_title_response(
        long job_title_id,
        String job_title_code,
        String job_title_name,
        String description,
        String status) {
}
