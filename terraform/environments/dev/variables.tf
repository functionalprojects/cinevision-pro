variable "project_name" { type = string }
variable "aws_profile" {
  type = string
  validation {
    condition     = length(trim(var.aws_profile, " ")) > 0
    error_message = "aws_profile must be set to a valid AWS CLI profile."
  }
}
variable "primary_region" { type = string }
variable "dr_region" { type = string }
variable "cidr_block" { type = string }
variable "azs" { type = list(string) }
variable "public_subnet_cidrs" { type = list(string) }
variable "private_subnet_cidrs" { type = list(string) }
variable "database_subnet_cidrs" { type = list(string) }
variable "frontend_bucket_name" {
  type = string
  validation {
    condition     = !strcontains(var.frontend_bucket_name, "BUCKET_NAME")
    error_message = "frontend_bucket_name still contains BUCKET_NAME placeholder."
  }
}
variable "logs_bucket_name" {
  type = string
  validation {
    condition     = !strcontains(var.logs_bucket_name, "BUCKET_NAME")
    error_message = "logs_bucket_name still contains BUCKET_NAME placeholder."
  }
}
variable "movie_posters_bucket_name" {
  type = string
  validation {
    condition     = !strcontains(var.movie_posters_bucket_name, "BUCKET_NAME")
    error_message = "movie_posters_bucket_name still contains BUCKET_NAME placeholder."
  }
}
variable "email_archives_bucket_name" {
  type = string
  validation {
    condition     = !strcontains(var.email_archives_bucket_name, "BUCKET_NAME")
    error_message = "email_archives_bucket_name still contains BUCKET_NAME placeholder."
  }
}
variable "frontend_aliases" {
  type = list(string)
  validation {
    condition     = alltrue([for a in var.frontend_aliases : !strcontains(a, "DOMAIN_NAME")])
    error_message = "frontend_aliases contains DOMAIN_NAME placeholder."
  }
}
variable "acm_certificate_arn" {
  type = string
  validation {
    condition     = !strcontains(var.acm_certificate_arn, "ACCOUNT_ID") && !strcontains(var.acm_certificate_arn, "CERTIFICATE_ID")
    error_message = "acm_certificate_arn still contains ACCOUNT_ID/CERTIFICATE_ID placeholder."
  }
}
variable "node_groups" {
  type = map(object({
    instance_types = list(string)
    desired_size   = number
    min_size       = number
    max_size       = number
    disk_size      = number
    capacity_type  = string
  }))
}
variable "rds_instance_class" { type = string }
variable "db_name" { type = string }
variable "db_username" { type = string }
variable "docdb_master_username" { type = string }
variable "database_credentials_secret_name" { type = string }
variable "application_secrets_secret_name" { type = string }
variable "redis_node_type" { type = string }
variable "msk_broker_count" { type = number }
variable "msk_broker_instance_type" { type = string }
variable "tags" {
  type    = map(string)
  default = {}
}
