variable "project_name" { type = string }
variable "aws_profile" {
  type = string
  validation {
    condition     = length(trim(var.aws_profile, " ")) > 0
    error_message = "aws_profile must be set to a valid AWS CLI profile."
  }
}
variable "primary_region" { type = string }
variable "prod_state_bucket" {
  type = string
  validation {
    condition     = !strcontains(var.prod_state_bucket, "ACCOUNT_ID")
    error_message = "prod_state_bucket still contains ACCOUNT_ID placeholder."
  }
}
variable "prod_state_key" { type = string }
variable "prod_state_region" { type = string }
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
variable "redis_node_type" { type = string }
variable "tags" {
  type    = map(string)
  default = {}
}
