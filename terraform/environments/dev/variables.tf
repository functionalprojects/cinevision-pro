variable "project_name" { type = string }
variable "aws_profile" { type = string }
variable "primary_region" { type = string }
variable "dr_region" { type = string }
variable "cidr_block" { type = string }
variable "azs" { type = list(string) }
variable "public_subnet_cidrs" { type = list(string) }
variable "private_subnet_cidrs" { type = list(string) }
variable "database_subnet_cidrs" { type = list(string) }
variable "frontend_bucket_name" { type = string }
variable "logs_bucket_name" { type = string }
variable "frontend_aliases" { type = list(string) }
variable "acm_certificate_arn" { type = string }
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
variable "db_password" {
  type      = string
  sensitive = true
}
variable "docdb_master_username" { type = string }
variable "docdb_master_password" {
  type      = string
  sensitive = true
}
variable "redis_node_type" { type = string }
variable "msk_broker_count" { type = number }
variable "msk_broker_instance_type" { type = string }
variable "tags" {
  type    = map(string)
  default = {}
}
