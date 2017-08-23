#' @importFrom rJava .jinit .jpackage J
#' 
.onLoad <- function(libname, pkgname) {
  .jpackage(pkgname, lib.loc = libname)
  if(J("java.lang.System")$getProperty("java.version") < "1.8.0") {
    stop("qCBA requires Java >= 1.8 ", call. = FALSE)
  }  
  .jinit()
}