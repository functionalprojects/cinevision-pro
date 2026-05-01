variable "project_name" {
  type    = string
  default = "cinevision"
}
variable "aws_profile" {
  type    = string
  default = "cinevision-staging"
  validation {
    condition     = length(trim(var.aws_profile, " ")) > 0
    error_message = "aws_profile must be set to a valid AWS CLI profile."
  }
}
variable "primary_region" {
  type    = string
  default = "us-east-1"
}
variable "dr_region" {
  type    = string
  default = "us-west-2"
}
variable "cidr_block" {
  type    = string
  default = "10.20.0.0/16"
}
variable "azs" {
  type    = list(string)
  default = ["us-east-1a", "us-east-1b", "us-east-1c"]
}
variable "public_subnet_cidrs" {
  type    = list(string)
  default = ["10.20.0.0/24", "10.20.1.0/24", "10.20.2.0/24"]
}
variable "private_subnet_cidrs" {
  type    = list(string)
  default = ["10.20.10.0/24", "10.20.11.0/24", "10.20.12.0/24"]
}
variable "database_subnet_cidrs" {
  type    = list(string)
  default = ["10.20.20.0/24", "10.20.21.0/24", "10.20.22.0/24"]
}
variable "frontend_bucket_name" {
  type    = string
  default = "staging-cinevision-staging-frontend"
  validation {
    condition     = !strcontains(var.frontend_bucket_name, "BUCKET_NAME")
    error_message = "frontend_bucket_name still contains BUCKET_NAME placeholder."
  }
}
variable "logs_bucket_name" {
  type    = string
  default = "staging-cinevision-staging-logs"
  validation {
    condition     = !strcontains(var.logs_bucket_name, "BUCKET_NAME")
    error_message = "logs_bucket_name still contains BUCKET_NAME placeholder."
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
  default = {
    general = {
      instance_types = ["m5.large"]
      desired_size   = 3
      min_size       = 3
      max_size       = 6
      disk_size      = 80
      capacity_type  = "ON_DEMAND"
    }
  }
}
variable "rds_instance_class" {
  type    = string
  default = "db.t4g.large"
}
variable "db_name" {
  type    = string
  default = "cinevision"
}
variable "db_username" {
  type    = string
  default = "cinevision_admin"
}
variable "docdb_master_username" {
  type    = string
  default = "cinevision_docdb_admin"
}
variable "database_credentials_secret_name" {
  type    = string
  default = "/cinevision/staging/database-credentials"
}
variable "application_secrets_secret_name" {
  type    = string
  default = "/cinevision/staging/application-secrets"
}
variable "redis_node_type" {
  type    = string
  default = "cache.t4g.medium"
}
variable "msk_broker_count" {
  type    = number
  default = 3
}
variable "msk_broker_instance_type" {
  type    = string
  default = "kafka.m5.large"
}
variable "tags" {
  type = map(string)
  default = {
    Owner = "platform-team"
  }
}
