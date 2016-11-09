---
layout: default
title: "Puppet Server: Running From Source"
canonical: "/puppetserver/latest/dev_running_from_source.html"
---

So you'd like to run Puppet Server from source?
-----

The following steps will help you get Puppet Server up and running from source.

Step 0: Quick Start for Developers
-----

    # clone git repository and initialize submodules
    $ git clone --recursive git://github.com/puppetlabs/puppetserver
    $ cd puppetserver

    # Remove any old config if you want to make sure you're using the latest
    # defaults
    $ rm -rf ~/.puppetserver

    # Run the `dev-setup` script to initialize all required configuration
    $ ./dev-setup

    # Launch the clojure REPL
    $ lein repl
    # Run Puppet Server
    user=> (go)
    user=> (help)

You should now have a running server.  All relevant paths (`$confdir`, `$codedir`,
etc.) are configured by default to point to directories underneath `~.puppetlabs`.
These should all align with the default values that `puppet` uses (for non-root
users).

You can find the specific paths in the `dev/puppetserver.conf` file.

In another shell, you can run the agent:

    # Go to the directory where you checked out puppetserver
    $ cd puppetserver
    # Set ruby and bin paths
    $ export RUBYLIB=./ruby/puppet/lib:./ruby/facter/lib
    $ export PATH=./ruby/puppet/bin:./ruby/facter/bin:$PATH
    # Run the agent
    $ puppet agent -t

More detailed instructions follow.

Step 1: Install Prerequisites
-----

Use your system's package tools to ensure that the following prerequisites are installed:

* JDK 1.7 or higher
* [Leiningen](http://leiningen.org/)
* Git (for checking out the source code)


Step 2: Clone Git Repo and Set Up Working Tree
-----

    $ git clone --recursive git://github.com/puppetlabs/puppetserver
    $ cd puppetserver

Step 3: Set up Config Files
-----

Choose a directory outside of the git working copy, e.g. `~/.puppetserver`, where you'd
like to maintain local config files for development.  Then, copy the file
`dev/puppetserver.conf.sample` to that directory, and name it `puppetserver.conf`.
Edit it to suit your needs.  A few notes:

* Relative paths in the config file refer to the root directory of the puppetserver
  git working copy.
* Puppet Server reads much of its configuration data from `puppet.conf` and Puppet's
  usual `confdir`.  The setting `master-conf-dir` may be used to modify the location
  of the Puppet `confdir`.  Otherwise, it will use the same default locations as
  Puppet normally uses (`~/.puppet` for non-root users, `/etc/puppet` for root).
* Similar to `confdir`, you will likely want to specify the other top-level
  Puppet directories `codedir`, `vardir`, `rundir`, and `logdir`.

You probably don't need to make any changes to the sample config for your first run,
but the settings that I edit the most frequently are:

 * `jruby-puppet.master-conf-dir`: the puppet master confdir (where `puppet.conf`,
   `modules`, `manifests`, etc. should be located).
 * `jruby-puppet.master-code-dir`: the puppet master codedir
 * `jruby-puppet.master-var-dir`: the puppet master vardir
 * `jruby-puppet.master-run-dir`: the puppet master rundir
 * `jruby-puppet.master-log-dir`: the puppet master logdir
 * `jruby-puppet.max-active-instances`: the number of JRuby instances to put into the
   pool.  This can usually be set to 1 for dev purposes, unless you're working on
   something that involves concurrency.

If you prefer, you may break the individual configuration sections from `puppetserver.conf`
into multiple `.conf` files, and place them in a `conf.d`-style directory.

Step 4a: Run the server from the clojure REPL
-----

The preferred way of running the server for development purposes is to run it from
inside the clojure REPL.  The git repo includes some files in the `/dev` directory
that are intended to make this process easier.

When running a clojure REPL via the `lein repl` command-line command, lein will look
for a namespace called `user.clj` on the classpath, and if one is found, it will
load that namespace by default.  We provide a sample version of this namespace that
is tailored for use with Puppet Server.  To use it, simply do:

    $ cp dev/user.clj.sample dev/user.clj

The git repo contains a `.gitignore` for `dev/user.clj`, so each user can customize
their user namespace to their liking.

Once you've made a copy of the `user.clj` file you'll want to edit it.  It contains
some comments explaining the contents of the file.  The main change you'll want to
make is to edit the `puppetserver-conf` function to your liking; you'll probably just
want to change it so that it points to the path of the `puppetserver.conf` file that
you created in step 3.

Running the server inside of the clojure REPL allows you to make changes to the
source code and reload the server without having to restart the entire JVM.  It
can be much faster than running from the command line, when you are doing iterative
development.  We are also starting to build up a library of utility functions that
can be used to inspect and modify the state of the running server; see `dev/user_repl.clj`
for more info.

(NOTE: many of the developers of this project are using a more full-featured IDE called
[Cursive Clojure](https://cursiveclojure.com/), built on the IntelliJ IDEA platform, for
our daily development.  It contains an integrated REPL that can be used in place of
the `lein repl` command-line command, and works great with all of the functions described
in this document.)

To start the server from the REPL, run the following:

    $ lein repl
    nREPL server started on port 47631 on host 127.0.0.1
    user=> (go)
    user=> (help)


Then, if you make changes to the source code, all you need to do in order to
restart the server with the latest changes is:

    user-repl=> (reset)

Restarting the server this way should be significantly faster than restarting
the entire JVM process.

You can also run the utility functions to inspect the state of the server, e.g.:

    user-repl=> (print-puppet-environment-states)

Have a look at `user_repl.clj` if you're interested in seeing what other utility
functions are available.

Step 4b: Run the server from the command line
-----

If you prefer not to run the server interactively in the REPL, you can launch it
as a normal process.  To start the Puppet Server when running from source, simply
run the following:

    $ lein run -c /path/to/puppetserver.conf

Running tests
-----

* `lein test` to run the clojure test suite
* `rake spec` to run the jruby test suite

The Clojure test suite can consume a lot of transient memory.  Using a larger
JVM heap size when running tests can significantly improve test run time.  The
default heap size is somewhat conservative: 1 GB for the minimum heap (much
lower than that as a maximum can lead to Java OutOfMemory errors during the
test run) and 2 GB for the maximum heap.  While the heap size can be configured
via the `-Xms` and `-Xmx` arguments for the `:jvm-opts` `defproject` key within
the `project.clj` file, it can also be customized for an individual user
environment via either of the following methods:

1) An environment variable named `PUPPETSERVER_HEAP_SIZE`.  For example, to
  use a heap size of 6 GiB for a `lein test` run, you could run the following:
  
    $ PUPPETSERVER_HEAP_SIZE=6G lein test
  
2) A lein `profiles.clj` setting in the `:user` profile under the
  `:puppetserver-heap-size` key.  For example, to use a heap size of 6 GiB, you
  could add the following key to your `~/.lein/profiles.clj` file:
  
```clj
{:user {:puppetserver-heap-size "6G"
        ...}}

```

With the `:puppetserver-heap-size` key defined in the `profiles.clj` file, any
subsequent `lein test` run would utilize the associated value for the key.  If
both the environment variable and the `profiles.clj` key are defined, the
value from the environment variable takes precedence.  When either of these
settings is defined, the value is used as both the minimum and maximum JVM heap
size.

From anecdotal testing from the puppetserver master branch as of 10/26/2016,
it appeared that at least a heap size of 5 GB would provide the best performance
benefit for full runs of the Clojure unit test suite.  This value may change
over time depending upon how the tests evolve.

Installing Ruby Gems for Development
-----

Please see the [Gems](./gems.markdown) document for detailed information on
installing Ruby Gems for use in development and testing contexts.

Debugging
------

For more information about debugging both Clojure and JRuby code, please see
[Puppet Server: Debugging](./dev_debugging.markdown) documentation.
