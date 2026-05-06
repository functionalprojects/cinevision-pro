terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.45"
    }
  }
}

provider "aws" {
  profile = var.aws_profile
  region  = var.primary_region
}

data "terraform_remote_state" "prod" {
  backend = "s3"

  config = {
    bucket  = var.prod_state_bucket
    key     = var.prod_state_key
    region  = var.prod_state_region
    profile = var.aws_profile
  }
}

locals {
  environment = "prod-dr"
  tags = merge(var.tags, {
    Environment = local.environment
    CostCenter  = "production"
    Tier        = "warm-standby"
  })
}

module "eks" {
  source = "../../modules/eks"

  cluster_name               = "${var.project_name}-${local.environment}"
  cluster_version            = "1.30"
  subnet_ids                 = data.terraform_remote_state.prod.outputs.dr_private_subnet_ids
  vpc_id                     = data.terraform_remote_state.prod.outputs.dr_vpc_id
  cluster_security_group_ids = [data.terraform_remote_state.prod.outputs.dr_eks_cluster_security_group_id]
  node_security_group_ids    = [data.terraform_remote_state.prod.outputs.dr_app_nodes_security_group_id]
  node_groups                = var.node_groups
  tags                       = local.tags
}

module "redis" {
  source = "../../modules/redis"

  replication_group_id = "${var.project_name}-${local.environment}-redis"
  node_type            = var.redis_node_type
  subnet_ids           = data.terraform_remote_state.prod.outputs.dr_database_subnet_ids
  security_group_ids   = [data.terraform_remote_state.prod.outputs.dr_redis_security_group_id]
  tags                 = local.tags
}
