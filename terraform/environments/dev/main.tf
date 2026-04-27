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

provider "aws" {
  alias   = "dr"
  profile = var.aws_profile
  region  = var.dr_region
}

locals {
  environment = "dev"
  tags = merge(var.tags, {
    Environment = local.environment
    CostCenter  = "engineering"
  })
}

module "vpc" {
  source = "../../modules/vpc"

  project_name          = var.project_name
  environment           = local.environment
  cidr_block            = var.cidr_block
  azs                   = var.azs
  public_subnet_cidrs   = var.public_subnet_cidrs
  private_subnet_cidrs  = var.private_subnet_cidrs
  database_subnet_cidrs = var.database_subnet_cidrs
  single_nat_gateway    = true
  enable_nat_gateway    = true
  tags                  = local.tags
}

module "s3_cloudfront" {
  source = "../../modules/s3-cloudfront"

  providers = {
    aws    = aws
    aws.dr = aws.dr
  }

  project_name         = var.project_name
  environment          = local.environment
  frontend_bucket_name = var.frontend_bucket_name
  logs_bucket_name     = var.logs_bucket_name
  aliases              = var.frontend_aliases
  acm_certificate_arn  = var.acm_certificate_arn
  enable_replication   = false
  tags                 = local.tags
}

module "eks" {
  source = "../../modules/eks"

  cluster_name               = "${var.project_name}-${local.environment}"
  cluster_version            = "1.30"
  subnet_ids                 = module.vpc.private_subnet_ids
  vpc_id                     = module.vpc.vpc_id
  cluster_security_group_ids = [module.vpc.eks_cluster_security_group_id]
  node_security_group_ids    = [module.vpc.app_nodes_security_group_id]
  node_groups                = var.node_groups
  tags                       = local.tags
}

module "rds" {
  source = "../../modules/rds"

  providers = {
    aws    = aws
    aws.dr = aws.dr
  }

  identifier              = "${var.project_name}-${local.environment}-postgres"
  db_name                 = var.db_name
  username                = var.db_username
  password                = var.db_password
  instance_class          = var.rds_instance_class
  subnet_ids              = module.vpc.database_subnet_ids
  security_group_ids      = [module.vpc.rds_security_group_id]
  create_dr_replica       = false
  backup_retention_period = 7
  tags                    = local.tags
}

module "documentdb" {
  source = "../../modules/documentdb"

  providers = {
    aws    = aws
    aws.dr = aws.dr
  }

  cluster_name          = "${var.project_name}-${local.environment}-docdb"
  master_username       = var.docdb_master_username
  master_password       = var.docdb_master_password
  subnet_ids            = module.vpc.database_subnet_ids
  security_group_ids    = [module.vpc.documentdb_security_group_id]
  enable_global_cluster = false
  instance_count        = 2
  tags                  = local.tags
}

module "redis" {
  source = "../../modules/redis"

  replication_group_id = "${var.project_name}-${local.environment}-redis"
  node_type            = var.redis_node_type
  subnet_ids           = module.vpc.database_subnet_ids
  security_group_ids   = [module.vpc.redis_security_group_id]
  tags                 = local.tags
}

module "msk" {
  source = "../../modules/msk"

  cluster_name           = "${var.project_name}-${local.environment}-msk"
  number_of_broker_nodes = var.msk_broker_count
  broker_instance_type   = var.msk_broker_instance_type
  subnet_ids             = module.vpc.private_subnet_ids
  security_group_ids     = [module.vpc.msk_security_group_id]
  tags                   = local.tags
}
