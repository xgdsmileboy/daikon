diff -urb --unidirectional-new-file -x.svn valgrind-old valgrind-new > coregrind.patch

diff -urb --unidirectional-new-file -x.svn -xdocs -xtests valgrind-old/memcheck valgrind-new/memcheck > memcheck.patch

diff -urb -x.git -x.hg -xinst -x.svn -xfjalar -xMakefile.in -xfjalar/html valgrind-old fjalar/valgrind > coregrind-PLSE.diff

diff -urb -x.git -x.hg -xinst -x.svn -xMakefile.in -xhtml -xdocs -xtests valgrind-old/memcheck fjalar/valgrind/fjalar > memcheck-PLSE.diff

