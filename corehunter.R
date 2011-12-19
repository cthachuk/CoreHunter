##############################
# load CoreHunter into R #
##############################

# define R function to run Corehunter
corehunter.run <- function(options){
 system(paste("java -jar bin/corehunter-cli.jar", options))
}