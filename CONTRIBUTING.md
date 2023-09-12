# Contributing

## IDE and code style

There is no restrictions on IDE you use as long as you follow existing code style of project.

Author uses IntelliJ IDEA, so it will be the easiest option for you as well. Make sure that you
import [code style settings](./.ide/intellij-codestyle.xml) into your IDEA, to make sure that you don't produce
occasional code reformatting.

### Rules of thumb for PRs

1. It can be a good idea to start with issue, rather than PR - let's discuss your needs first and then proceed to code
   changes.
2. One PR - one purpose of change. This will simplify review and speed up merges. Avoid doing all the things in one
   branch that will be a source of one PR.
3. Dependencies - it is fine to add new dependencies if they are required for the purpose of feature you are working
   on, but please refrain from adding/removing deps:
    - just for kicks
    - by replacing specific dependencies with "catch all" deps, (e.g. replacing specific flexmark modules
      with `flexmark-all`)
4. Small cleanups along the way are fine to do in scope of your main PR, but if you consider something large, like
   reformatting all the source code or massively renaming some class/term - do it in separate PR.
5. IntelliJ IDEA users - I suggest you to enable "optimize imports" on commit and also reformatting code only for
   changed lines by default.